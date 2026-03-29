import { createHmac, randomUUID } from "node:crypto";

import type { Department, Designation, Employee, EmployeeInvite, PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";

import { HttpError } from "../common/errors.js";
import type { DepartmentCreateDto, DepartmentUpdateDto } from "../modules/department/dto.js";
import type { DesignationCreateDto, DesignationUpdateDto } from "../modules/designation/dto.js";
import type { EmployeeProfileUpdateDto, EmployeeRequestDto } from "../modules/employee/dto.js";
import type { CompleteRegistrationRequestDto, EmployeeInviteRequestDto } from "../modules/invite/dto.js";
import { generateFinalEmployeeId, generateTempEmployeeId } from "../utils/employee-id.js";
import { AuthSyncService } from "./auth-sync.service.js";
import type { AttendanceLeaveService } from "./attendance-leave.service.js";
import type { MediaStorageService } from "./media-storage.service.js";

type EmployeeWithRelations = Employee & {
  department: Department | null;
  designation: Designation | null;
  reportingTo: Employee | null;
};

const employeeInclude = {
  department: true,
  designation: true,
  reportingTo: true
} as const;

export class EmployeeService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly authSyncService: AuthSyncService,
    private readonly jwtSecret?: string,
    private readonly attendanceLeaveService?: AttendanceLeaveService,
    private readonly mediaStorageService?: MediaStorageService
  ) {}

  async sendInvite(request: EmployeeInviteRequestDto, invitedByEmployeeId?: string): Promise<Record<string, unknown>> {
    const email = request.email?.trim().toLowerCase() || request.to?.trim().toLowerCase();

    if (!email) {
      throw new HttpError(400, "email is required");
    }

    const existingEmployeeByEmail = await this.prisma.employee.findUnique({
      where: { email }
    });

    if (existingEmployeeByEmail && existingEmployeeByEmail.active) {
      throw new HttpError(409, "Active employee with this email already exists");
    }

    const employeeId = existingEmployeeByEmail?.employeeId ?? (await this.generateUniqueTempEmployeeId());
    const inviteToken = randomUUID();
    const expiresAt = new Date(Date.now() + 30 * 60 * 1000);

    await this.prisma.$transaction(async (tx) => {
      if (!existingEmployeeByEmail) {
        await tx.employee.create({
          data: {
            employeeId,
            email,
            name: "TEMP USER",
            about: "INVITED_USER",
            password: await hashPassword(randomUUID()),
            address: "NA",
            language: "NA",
            bloodGroup: "NA",
            country: "NA",
            gender: "NA",
            birthday: new Date(),
            loginAllowed: false,
            active: false,
            joiningDate: new Date()
          }
        });
      } else {
        await tx.employee.update({
          where: { employeeId },
          data: {
            loginAllowed: false,
            active: false,
            about: existingEmployeeByEmail.about || "INVITED_USER",
            name: existingEmployeeByEmail.name || "TEMP USER"
          }
        });
      }

      await tx.employeeInvite.updateMany({
        where: {
          email,
          used: false,
          expiresAt: { gt: new Date() }
        },
        data: {
          used: true,
          usedAt: new Date()
        }
      });

      await tx.employeeInvite.create({
        data: {
          employeeId,
          email,
          token: inviteToken,
          expiresAt
        }
      });
    });

    return {
      message: "Invite sent",
      employeeId,
      email,
      expiresAt,
      inviteToken,
      invitedByEmployeeId: invitedByEmployeeId ?? null
    };
  }

  async acceptInvite(token: string): Promise<Record<string, unknown>> {
    const normalizedToken = token.trim();

    if (!normalizedToken) {
      throw new HttpError(400, "token is required");
    }

    const invite = await this.prisma.employeeInvite.findUnique({
      where: { token: normalizedToken }
    });

    if (!invite) {
      throw new HttpError(404, "Invalid token");
    }

    if (invite.used || invite.expiresAt.getTime() < Date.now()) {
      throw new HttpError(400, "Invite expired");
    }

    await this.prisma.employeeInvite.update({
      where: { id: invite.id },
      data: {
        used: true,
        usedAt: new Date()
      }
    });

    return {
      token: this.signInviteToken(invite.employeeId, invite.email)
    };
  }

  async completeRegistration(
    authorizationHeader: string | undefined,
    request: CompleteRegistrationRequestDto
  ): Promise<Record<string, unknown>> {
    const claims = this.parseInviteAuthorization(authorizationHeader);
    const name = request.name?.trim();
    const password = request.password?.trim();

    if (!name || !password) {
      throw new HttpError(400, "name and password are required");
    }

    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: claims.sub }
    });

    if (!employee) {
      throw new HttpError(404, "Employee not found");
    }

    if (employee.active && employee.loginAllowed) {
      throw new HttpError(409, "Registration already completed");
    }

    const finalEmployeeId = await this.generateUniqueFinalEmployeeId();

    const updatedEmployee = await this.prisma.employee.update({
      where: { employeeId: employee.employeeId },
      data: {
        employeeId: finalEmployeeId,
        name,
        mobile: request.mobile?.trim() || null,
        gender: request.gender?.trim() || null,
        birthday: request.birthday ? toDate(request.birthday) : null,
        address: request.address?.trim() || null,
        password: await hashPassword(password),
        loginAllowed: true,
        active: true
      }
    });

    await this.authSyncService.register(finalEmployeeId, password, updatedEmployee.role, updatedEmployee.email);
    await this.attendanceLeaveService?.assignDefaultLeaveQuotas(finalEmployeeId);

    return {
      forceLogout: true,
      employeeId: finalEmployeeId
    };
  }

  async changeEmployeeId(employeeId: string, newEmployeeId?: string): Promise<Record<string, string>> {
    const existingEmployee = await this.findEmployeeOrThrow(employeeId);
    const normalizedNewEmployeeId = normalizeEmployeeId(newEmployeeId ?? "");

    if (!normalizedNewEmployeeId) {
      throw new HttpError(400, "newEmployeeId is required");
    }

    if (existingEmployee.loginAllowed) {
      throw new HttpError(400, "Cannot change employeeId after login is enabled");
    }

    const collision = await this.prisma.employee.findUnique({
      where: { employeeId: normalizedNewEmployeeId },
      select: { employeeId: true }
    });

    if (collision) {
      throw new HttpError(409, "Employee ID already exists");
    }

    await this.prisma.$transaction(async (tx) => {
      await tx.employee.update({
        where: { employeeId: existingEmployee.employeeId },
        data: { employeeId: normalizedNewEmployeeId }
      });

      await tx.employeeInvite.updateMany({
        where: { employeeId: existingEmployee.employeeId },
        data: { employeeId: normalizedNewEmployeeId }
      });
    });

    return { message: "EmployeeId updated" };
  }

  async createEmployee(
    request: EmployeeRequestDto,
    profilePictureFile?: { filename: string | null; contentType: string | null; data: Buffer } | null
  ): Promise<Record<string, unknown>> {
    const name = request.name?.trim();
    const email = request.email?.trim().toLowerCase();

    if (!name || !email) {
      throw new HttpError(400, "name and email are required");
    }

    const employeeId = normalizeEmployeeId(request.employeeId ?? generateFinalEmployeeId());

    const existingEmployee = await this.prisma.employee.findUnique({
      where: { employeeId }
    });

    if (existingEmployee) {
      throw new HttpError(409, "Employee ID already exists");
    }

    const role = normalizeRole(request.role);
    const loginAllowed = request.loginAllowed ?? true;
    const receiveEmailNotification = request.receiveEmailNotification ?? true;
    const plainPassword = request.password?.trim() || null;

    if (loginAllowed && !plainPassword) {
      throw new HttpError(400, "password is required when loginAllowed is true");
    }

    await this.ensureRelatedEntitiesExist(request);

    const uploadedProfilePicture = await this.mediaStorageService?.saveUploadedFile(
      profilePictureFile,
      `employees/${employeeId}/profile`
    );
    const employee = await this.prisma.employee.create({
      data: {
        employeeId,
        name,
        email,
        password: plainPassword ? await hashPassword(plainPassword) : null,
        profilePictureUrl: uploadedProfilePicture?.url ?? (request.profilePictureUrl?.trim() || null),
        gender: request.gender?.trim() || null,
        birthday: toDate(request.birthday),
        bloodGroup: request.bloodGroup?.trim() || null,
        joiningDate: toDate(request.joiningDate),
        language: request.language?.trim() || null,
        country: request.country?.trim() || null,
        mobile: request.mobile?.trim() || null,
        address: request.address?.trim() || null,
        about: request.about?.trim() || null,
        departmentId: toBigInt(request.departmentId),
        designationId: toBigInt(request.designationId),
        reportingToId: request.reportingToId ? normalizeEmployeeId(request.reportingToId) : null,
        role,
        loginAllowed,
        receiveEmailNotification,
        hourlyRate: request.hourlyRate ?? null,
        slackMemberId: request.slackMemberId?.trim() || null,
        skills: sanitizeSkills(request.skills),
        probationEndDate: toDate(request.probationEndDate),
        noticePeriodStartDate: toDate(request.noticePeriodStartDate),
        noticePeriodEndDate: toDate(request.noticePeriodEndDate),
        employmentType: request.employmentType?.trim() || null,
        maritalStatus: request.maritalStatus?.trim() || null,
        businessAddress: request.businessAddress?.trim() || null,
        officeShift: request.officeShift?.trim() || null,
        active: true
      },
      include: employeeInclude
    });

    if (loginAllowed && plainPassword) {
      await this.authSyncService.register(employee.employeeId, plainPassword, employee.role, employee.email);
    }

    await this.attendanceLeaveService?.assignDefaultLeaveQuotas(employee.employeeId);

    return mapEmployee(employee);
  }

  async listEmployees(): Promise<Record<string, unknown>[]> {
    const employees = await this.prisma.employee.findMany({
      include: employeeInclude,
      orderBy: { createdAt: "desc" }
    });

    return employees.map(mapEmployee);
  }

  async listEmployeesPage(page = 1, pageSize = 20): Promise<Record<string, unknown>> {
    const normalizedPage = Math.max(1, page);
    const normalizedPageSize = Math.max(1, Math.min(100, pageSize));
    const [items, total] = await this.prisma.$transaction([
      this.prisma.employee.findMany({
        include: employeeInclude,
        orderBy: { createdAt: "desc" },
        skip: (normalizedPage - 1) * normalizedPageSize,
        take: normalizedPageSize
      }),
      this.prisma.employee.count()
    ]);

    return {
      content: items.map(mapEmployee),
      page: normalizedPage,
      pageSize: normalizedPageSize,
      totalElements: total,
      totalPages: Math.ceil(total / normalizedPageSize)
    };
  }

  async getEmployee(employeeId: string): Promise<Record<string, unknown>> {
    const employee = await this.findEmployeeOrThrow(employeeId);
    return mapEmployee(employee);
  }

  async getEmployeeMeta(employeeId: string): Promise<Record<string, unknown>> {
    const employee = await this.findEmployeeOrThrow(employeeId);

    return mapEmployeeMeta(employee);
  }

  async employeeExists(employeeId: string): Promise<boolean> {
    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: normalizeEmployeeId(employeeId) },
      select: { employeeId: true }
    });

    return Boolean(employee);
  }

  async searchEmployeeMeta(query: string): Promise<Record<string, unknown>[]> {
    const normalizedQuery = query.trim();

    if (!normalizedQuery) {
      return [];
    }

    const employees = await this.prisma.employee.findMany({
      where: {
        OR: [
          { name: { contains: normalizedQuery, mode: "insensitive" } },
          { email: { contains: normalizedQuery, mode: "insensitive" } }
        ]
      },
      include: employeeInclude,
      orderBy: { name: "asc" },
      take: 20
    });

    return employees.map(mapEmployeeMeta);
  }

  async getEmployeesWithBirthday(date?: string | null): Promise<Record<string, unknown>[]> {
    const targetDate = date ? requireBirthdayDate(date) : new Date();
    const month = targetDate.getUTCMonth() + 1;
    const day = targetDate.getUTCDate();

    const employees = await this.prisma.employee.findMany({
      where: {
        birthday: {
          not: null
        }
      },
      include: employeeInclude,
      orderBy: { name: "asc" }
    });

    return employees
      .filter((employee) => {
        if (!employee.birthday) {
          return false;
        }

        return employee.birthday.getUTCMonth() + 1 === month && employee.birthday.getUTCDate() === day;
      })
      .map(mapEmployee);
  }

  async updateMyProfile(
    employeeId: string,
    request: EmployeeProfileUpdateDto,
    profilePictureFile?: { filename: string | null; contentType: string | null; data: Buffer } | null
  ): Promise<Record<string, unknown>> {
    const existingEmployee = await this.findEmployeeOrThrow(employeeId);
    const nextEmail = request.email?.trim().toLowerCase() ?? existingEmployee.email;
    const nextMobile =
      request.mobile !== undefined ? normalizeOptionalString(request.mobile, existingEmployee.mobile) : existingEmployee.mobile;

    await this.ensureUniqueEmployeeFields(existingEmployee.employeeId, nextEmail, nextMobile);

    const uploadedProfilePicture = await this.mediaStorageService?.saveUploadedFile(
      profilePictureFile,
      `employees/${existingEmployee.employeeId}/profile`
    );
    const updatedEmployee = await this.prisma.employee.update({
      where: { employeeId: existingEmployee.employeeId },
      data: {
        name: request.name?.trim() ?? existingEmployee.name,
        email: nextEmail,
        profilePictureUrl:
          uploadedProfilePicture?.url ?? normalizeOptionalString(request.profilePictureUrl, existingEmployee.profilePictureUrl),
        gender: normalizeOptionalString(request.gender, existingEmployee.gender),
        birthday: request.birthday !== undefined ? toDate(request.birthday) : existingEmployee.birthday,
        bloodGroup: normalizeOptionalString(request.bloodGroup, existingEmployee.bloodGroup),
        language: normalizeOptionalString(request.language, existingEmployee.language),
        country: normalizeOptionalString(request.country, existingEmployee.country),
        mobile: nextMobile,
        address: normalizeOptionalString(request.address, existingEmployee.address),
        about: normalizeOptionalString(request.about, existingEmployee.about),
        slackMemberId: normalizeOptionalString(request.slackMemberId, existingEmployee.slackMemberId),
        maritalStatus: normalizeOptionalString(request.maritalStatus, existingEmployee.maritalStatus)
      },
      include: employeeInclude
    });

    if (existingEmployee.email !== updatedEmployee.email && updatedEmployee.loginAllowed) {
      await this.authSyncService.updateEmail(updatedEmployee.employeeId, updatedEmployee.email);
    }

    return mapEmployee(updatedEmployee);
  }

  async updateEmployee(
    employeeId: string,
    request: EmployeeRequestDto,
    profilePictureFile?: { filename: string | null; contentType: string | null; data: Buffer } | null
  ): Promise<Record<string, unknown>> {
    const existingEmployee = await this.findEmployeeOrThrow(employeeId);
    const nextRole = request.role ? normalizeRole(request.role) : existingEmployee.role;
    const nextEmail = request.email?.trim().toLowerCase() ?? existingEmployee.email;
    const nextLoginAllowed = request.loginAllowed ?? existingEmployee.loginAllowed;
    const nextPassword = request.password?.trim();

    if (!existingEmployee.loginAllowed && nextLoginAllowed && !nextPassword) {
      throw new HttpError(400, "password is required when enabling login");
    }

    await this.ensureRelatedEntitiesExist(request);

    const uploadedProfilePicture = await this.mediaStorageService?.saveUploadedFile(
      profilePictureFile,
      `employees/${existingEmployee.employeeId}/profile`
    );
    const updatedEmployee = await this.prisma.employee.update({
      where: { employeeId: existingEmployee.employeeId },
      data: {
        name: request.name?.trim() ?? existingEmployee.name,
        email: nextEmail,
        password: nextPassword ? await hashPassword(nextPassword) : existingEmployee.password,
        profilePictureUrl:
          uploadedProfilePicture?.url ?? normalizeOptionalString(request.profilePictureUrl, existingEmployee.profilePictureUrl),
        gender: normalizeOptionalString(request.gender, existingEmployee.gender),
        birthday: request.birthday !== undefined ? toDate(request.birthday) : existingEmployee.birthday,
        bloodGroup: normalizeOptionalString(request.bloodGroup, existingEmployee.bloodGroup),
        joiningDate: request.joiningDate !== undefined ? toDate(request.joiningDate) : existingEmployee.joiningDate,
        language: normalizeOptionalString(request.language, existingEmployee.language),
        country: normalizeOptionalString(request.country, existingEmployee.country),
        mobile: normalizeOptionalString(request.mobile, existingEmployee.mobile),
        address: normalizeOptionalString(request.address, existingEmployee.address),
        about: normalizeOptionalString(request.about, existingEmployee.about),
        departmentId: request.departmentId !== undefined ? toBigInt(request.departmentId) : existingEmployee.departmentId,
        designationId: request.designationId !== undefined ? toBigInt(request.designationId) : existingEmployee.designationId,
        reportingToId:
          request.reportingToId !== undefined
            ? request.reportingToId
              ? normalizeEmployeeId(request.reportingToId)
              : null
            : existingEmployee.reportingToId,
        role: nextRole,
        loginAllowed: nextLoginAllowed,
        receiveEmailNotification: request.receiveEmailNotification ?? existingEmployee.receiveEmailNotification,
        hourlyRate: request.hourlyRate !== undefined ? request.hourlyRate : existingEmployee.hourlyRate,
        slackMemberId: normalizeOptionalString(request.slackMemberId, existingEmployee.slackMemberId),
        skills: request.skills ? sanitizeSkills(request.skills) : existingEmployee.skills,
        probationEndDate:
          request.probationEndDate !== undefined ? toDate(request.probationEndDate) : existingEmployee.probationEndDate,
        noticePeriodStartDate:
          request.noticePeriodStartDate !== undefined
            ? toDate(request.noticePeriodStartDate)
            : existingEmployee.noticePeriodStartDate,
        noticePeriodEndDate:
          request.noticePeriodEndDate !== undefined
            ? toDate(request.noticePeriodEndDate)
            : existingEmployee.noticePeriodEndDate,
        employmentType: normalizeOptionalString(request.employmentType, existingEmployee.employmentType),
        maritalStatus: normalizeOptionalString(request.maritalStatus, existingEmployee.maritalStatus),
        businessAddress: normalizeOptionalString(request.businessAddress, existingEmployee.businessAddress),
        officeShift: normalizeOptionalString(request.officeShift, existingEmployee.officeShift)
      },
      include: employeeInclude
    });

    if (!existingEmployee.loginAllowed && updatedEmployee.loginAllowed && nextPassword) {
      await this.authSyncService.register(updatedEmployee.employeeId, nextPassword, updatedEmployee.role, updatedEmployee.email);
    }

    if (existingEmployee.loginAllowed && !updatedEmployee.loginAllowed) {
      await this.authSyncService.delete(updatedEmployee.employeeId);
    }

    if (existingEmployee.role !== updatedEmployee.role && updatedEmployee.loginAllowed) {
      await this.authSyncService.updateRole(updatedEmployee.employeeId, updatedEmployee.role);
    }

    if (nextPassword && updatedEmployee.loginAllowed) {
      await this.authSyncService.updatePassword(updatedEmployee.employeeId, nextPassword);
    }

    if (existingEmployee.email !== updatedEmployee.email && updatedEmployee.loginAllowed) {
      await this.authSyncService.updateEmail(updatedEmployee.employeeId, updatedEmployee.email);
    }

    return mapEmployee(updatedEmployee);
  }

  async updateEmployeeRole(employeeId: string, role?: string): Promise<Record<string, unknown>> {
    if (!role?.trim()) {
      throw new HttpError(400, "role is required");
    }

    return this.updateEmployee(employeeId, { role });
  }

  async deleteEmployee(employeeId: string): Promise<Record<string, string>> {
    const existingEmployee = await this.findEmployeeOrThrow(employeeId);

    await this.prisma.employee.delete({
      where: { employeeId: existingEmployee.employeeId }
    });

    if (existingEmployee.loginAllowed) {
      await this.authSyncService.delete(existingEmployee.employeeId);
    }

    return { status: "success" };
  }

  async listDepartments(): Promise<Record<string, unknown>[]> {
    const departments = await this.prisma.department.findMany({
      orderBy: { departmentName: "asc" }
    });

    return departments.map((department) => ({
      id: Number(department.id),
      departmentName: department.departmentName,
      parentDepartmentId: department.parentId ? Number(department.parentId) : null,
      createdAt: department.createdAt
    }));
  }

  async getDepartment(id: number): Promise<Record<string, unknown>> {
    const department = await this.prisma.department.findUnique({
      where: { id: BigInt(id) }
    });

    if (!department) {
      throw new HttpError(404, "Department not found");
    }

    return {
      id: Number(department.id),
      departmentName: department.departmentName,
      parentDepartmentId: department.parentId ? Number(department.parentId) : null,
      createdAt: department.createdAt
    };
  }

  async createDepartment(dto: DepartmentCreateDto): Promise<Record<string, unknown>> {
    if (!dto.departmentName?.trim()) {
      throw new HttpError(400, "departmentName is required");
    }

    if (dto.parentDepartmentId !== undefined && dto.parentDepartmentId !== null) {
      await this.ensureDepartmentExists(dto.parentDepartmentId);
    }

    const department = await this.prisma.department.create({
      data: {
        departmentName: dto.departmentName.trim(),
        parentId: toBigInt(dto.parentDepartmentId)
      }
    });

    return {
      id: Number(department.id),
      departmentName: department.departmentName,
      parentDepartmentId: department.parentId ? Number(department.parentId) : null,
      createdAt: department.createdAt
    };
  }

  async updateDepartment(id: number, dto: DepartmentUpdateDto): Promise<Record<string, unknown>> {
    if (dto.parentDepartmentId !== undefined && dto.parentDepartmentId === id) {
      throw new HttpError(400, "Department cannot be parent of itself");
    }

    if (dto.parentDepartmentId !== undefined && dto.parentDepartmentId !== null) {
      await this.ensureDepartmentExists(dto.parentDepartmentId);
    }

    const department = await this.prisma.department.update({
      where: { id: BigInt(id) },
      data: {
        departmentName: dto.departmentName?.trim(),
        parentId: dto.parentDepartmentId !== undefined ? toBigInt(dto.parentDepartmentId) : undefined
      }
    });

    return {
      id: Number(department.id),
      departmentName: department.departmentName,
      parentDepartmentId: department.parentId ? Number(department.parentId) : null,
      createdAt: department.createdAt
    };
  }

  async deleteDepartment(id: number): Promise<Record<string, string>> {
    await this.prisma.department.delete({
      where: { id: BigInt(id) }
    });

    return { status: "success" };
  }

  async listDesignations(): Promise<Record<string, unknown>[]> {
    const designations = await this.prisma.designation.findMany({
      orderBy: { designationName: "asc" }
    });

    return designations.map((designation) => ({
      id: Number(designation.id),
      designationName: designation.designationName,
      parentDesignationId: designation.parentId ? Number(designation.parentId) : null,
      createdAt: designation.createdAt
    }));
  }

  async getDesignation(id: number): Promise<Record<string, unknown>> {
    const designation = await this.prisma.designation.findUnique({
      where: { id: BigInt(id) }
    });

    if (!designation) {
      throw new HttpError(404, "Designation not found");
    }

    return {
      id: Number(designation.id),
      designationName: designation.designationName,
      parentDesignationId: designation.parentId ? Number(designation.parentId) : null,
      createdAt: designation.createdAt
    };
  }

  async createDesignation(dto: DesignationCreateDto): Promise<Record<string, unknown>> {
    if (!dto.designationName?.trim()) {
      throw new HttpError(400, "designationName is required");
    }

    if (dto.parentDesignationId !== undefined && dto.parentDesignationId !== null) {
      await this.ensureDesignationExists(dto.parentDesignationId);
    }

    const designation = await this.prisma.designation.create({
      data: {
        designationName: dto.designationName.trim(),
        parentId: toBigInt(dto.parentDesignationId)
      }
    });

    return {
      id: Number(designation.id),
      designationName: designation.designationName,
      parentDesignationId: designation.parentId ? Number(designation.parentId) : null,
      createdAt: designation.createdAt
    };
  }

  async updateDesignation(id: number, dto: DesignationUpdateDto): Promise<Record<string, unknown>> {
    if (dto.parentDesignationId !== undefined && dto.parentDesignationId === id) {
      throw new HttpError(400, "Designation cannot be parent of itself");
    }

    if (dto.parentDesignationId !== undefined && dto.parentDesignationId !== null) {
      await this.ensureDesignationExists(dto.parentDesignationId);
    }

    const designation = await this.prisma.designation.update({
      where: { id: BigInt(id) },
      data: {
        designationName: dto.designationName?.trim(),
        parentId: dto.parentDesignationId !== undefined ? toBigInt(dto.parentDesignationId) : undefined
      }
    });

    return {
      id: Number(designation.id),
      designationName: designation.designationName,
      parentDesignationId: designation.parentId ? Number(designation.parentId) : null,
      createdAt: designation.createdAt
    };
  }

  async deleteDesignation(id: number): Promise<Record<string, string>> {
    await this.prisma.designation.delete({
      where: { id: BigInt(id) }
    });

    return { status: "success" };
  }

  private async findEmployeeOrThrow(employeeId: string): Promise<EmployeeWithRelations> {
    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: normalizeEmployeeId(employeeId) },
      include: employeeInclude
    });

    if (!employee) {
      throw new HttpError(404, "Employee not found");
    }

    return employee;
  }

  private async ensureRelatedEntitiesExist(request: EmployeeRequestDto): Promise<void> {
    if (request.departmentId !== undefined && request.departmentId !== null) {
      await this.ensureDepartmentExists(request.departmentId);
    }

    if (request.designationId !== undefined && request.designationId !== null) {
      await this.ensureDesignationExists(request.designationId);
    }

    if (request.reportingToId) {
      await this.findEmployeeOrThrow(request.reportingToId);
    }
  }

  private async ensureDepartmentExists(id: number): Promise<void> {
    const department = await this.prisma.department.findUnique({
      where: { id: BigInt(id) },
      select: { id: true }
    });

    if (!department) {
      throw new HttpError(404, "Department not found");
    }
  }

  private async ensureDesignationExists(id: number): Promise<void> {
    const designation = await this.prisma.designation.findUnique({
      where: { id: BigInt(id) },
      select: { id: true }
    });

    if (!designation) {
      throw new HttpError(404, "Designation not found");
    }
  }

  private async ensureUniqueEmployeeFields(
    currentEmployeeId: string,
    email?: string | null,
    mobile?: string | null
  ): Promise<void> {
    if (email) {
      const employeeWithEmail = await this.prisma.employee.findFirst({
        where: {
          email,
          employeeId: { not: currentEmployeeId }
        },
        select: { employeeId: true }
      });

      if (employeeWithEmail) {
        throw new HttpError(409, "Email already exists");
      }
    }

    if (mobile) {
      const employeeWithMobile = await this.prisma.employee.findFirst({
        where: {
          mobile,
          employeeId: { not: currentEmployeeId }
        },
        select: { employeeId: true }
      });

      if (employeeWithMobile) {
        throw new HttpError(409, "Mobile already exists");
      }
    }
  }

  private async generateUniqueTempEmployeeId(): Promise<string> {
    while (true) {
      const candidate = generateTempEmployeeId();
      const existing = await this.prisma.employee.findUnique({
        where: { employeeId: candidate },
        select: { employeeId: true }
      });

      if (!existing) {
        return candidate;
      }
    }
  }

  private async generateUniqueFinalEmployeeId(): Promise<string> {
    while (true) {
      const candidate = normalizeEmployeeId(generateFinalEmployeeId());
      const existing = await this.prisma.employee.findUnique({
        where: { employeeId: candidate },
        select: { employeeId: true }
      });

      if (!existing) {
        return candidate;
      }
    }
  }

  private signInviteToken(employeeId: string, email: string): string {
    if (!this.jwtSecret) {
      throw new Error("JWT secret is not configured");
    }

    const claims: InviteTokenClaims = {
      sub: employeeId,
      email,
      temp: true,
      type: "invite",
      iat: Date.now(),
      exp: Date.now() + 30 * 60 * 1000
    };

    const payload = Buffer.from(JSON.stringify(claims)).toString("base64url");
    const signature = createHmac("sha256", this.jwtSecret).update(payload).digest("base64url");
    return `${payload}.${signature}`;
  }

  private parseInviteAuthorization(authorizationHeader?: string): InviteTokenClaims {
    if (!authorizationHeader?.startsWith("Bearer ")) {
      throw new HttpError(401, "Authentication required");
    }

    const token = authorizationHeader.slice("Bearer ".length).trim();
    const [payload, signature] = token.split(".");

    if (!payload || !signature || !this.jwtSecret) {
      throw new HttpError(401, "Malformed token");
    }

    const expectedSignature = createHmac("sha256", this.jwtSecret).update(payload).digest("base64url");

    if (expectedSignature !== signature) {
      throw new HttpError(401, "Invalid token signature");
    }

    const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as InviteTokenClaims;

    if (!claims.temp || claims.type !== "invite" || claims.exp < Date.now()) {
      throw new HttpError(401, "Invite token expired");
    }

    return claims;
  }
}

interface InviteTokenClaims {
  sub: string;
  email: string;
  temp: true;
  type: "invite";
  iat: number;
  exp: number;
}

function mapEmployee(employee: EmployeeWithRelations): Record<string, unknown> {
  return {
    employeeId: employee.employeeId,
    name: employee.name,
    email: employee.email,
    profilePictureUrl: employee.profilePictureUrl,
    gender: employee.gender,
    birthday: employee.birthday,
    bloodGroup: employee.bloodGroup,
    joiningDate: employee.joiningDate,
    language: employee.language,
    country: employee.country,
    mobile: employee.mobile,
    address: employee.address,
    about: employee.about,
    departmentId: employee.departmentId ? Number(employee.departmentId) : null,
    departmentName: employee.department?.departmentName ?? null,
    designationId: employee.designationId ? Number(employee.designationId) : null,
    designationName: employee.designation?.designationName ?? null,
    reportingToId: employee.reportingToId,
    reportingToName: employee.reportingTo?.name ?? null,
    role: employee.role,
    loginAllowed: employee.loginAllowed,
    receiveEmailNotification: employee.receiveEmailNotification,
    hourlyRate: employee.hourlyRate,
    slackMemberId: employee.slackMemberId,
    skills: employee.skills,
    probationEndDate: employee.probationEndDate,
    noticePeriodStartDate: employee.noticePeriodStartDate,
    noticePeriodEndDate: employee.noticePeriodEndDate,
    employmentType: employee.employmentType,
    maritalStatus: employee.maritalStatus,
    businessAddress: employee.businessAddress,
    officeShift: employee.officeShift,
    active: employee.active,
    createdAt: employee.createdAt
  };
}

function mapEmployeeMeta(employee: EmployeeWithRelations): Record<string, unknown> {
  return {
    employeeId: employee.employeeId,
    name: employee.name,
    designation: employee.designation?.designationName ?? null,
    department: employee.department?.departmentName ?? null,
    profileUrl: employee.profilePictureUrl
  };
}

function normalizeEmployeeId(employeeId: string): string {
  return employeeId.trim().toUpperCase();
}

function normalizeRole(role?: string | null): string {
  return role?.trim().toUpperCase() || "ROLE_EMPLOYEE";
}

function sanitizeSkills(skills?: string[] | null): string[] {
  return skills?.map((skill) => skill.trim()).filter(Boolean) ?? [];
}

function toDate(value?: string | null): Date | null {
  if (!value) {
    return null;
  }

  return new Date(value);
}

function toBigInt(value?: number | null): bigint | null {
  if (value === null || value === undefined) {
    return null;
  }

  return BigInt(value);
}

function normalizeOptionalString(incoming: string | null | undefined, fallback: string | null): string | null {
  if (incoming === undefined) {
    return fallback;
  }

  return incoming?.trim() || null;
}

async function hashPassword(password: string): Promise<string> {
  return bcrypt.hash(password, 10);
}

function requireBirthdayDate(value: string): Date {
  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    throw new HttpError(400, "Invalid date");
  }

  return parsed;
}
