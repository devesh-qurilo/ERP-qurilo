import type { Department, Designation, Employee, PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";

import { HttpError } from "../common/errors.js";
import type { DepartmentCreateDto, DepartmentUpdateDto } from "../modules/department/dto.js";
import type { DesignationCreateDto, DesignationUpdateDto } from "../modules/designation/dto.js";
import type { EmployeeRequestDto } from "../modules/employee/dto.js";
import { generateFinalEmployeeId } from "../utils/employee-id.js";
import { AuthSyncService } from "./auth-sync.service.js";

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
    private readonly authSyncService: AuthSyncService
  ) {}

  async createEmployee(request: EmployeeRequestDto): Promise<Record<string, unknown>> {
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

    const employee = await this.prisma.employee.create({
      data: {
        employeeId,
        name,
        email,
        password: plainPassword ? await hashPassword(plainPassword) : null,
        profilePictureUrl: request.profilePictureUrl?.trim() || null,
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

    return {
      employeeId: employee.employeeId,
      name: employee.name,
      designation: employee.designation?.designationName ?? null,
      department: employee.department?.departmentName ?? null,
      profileUrl: employee.profilePictureUrl
    };
  }

  async employeeExists(employeeId: string): Promise<boolean> {
    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: normalizeEmployeeId(employeeId) },
      select: { employeeId: true }
    });

    return Boolean(employee);
  }

  async updateEmployee(employeeId: string, request: EmployeeRequestDto): Promise<Record<string, unknown>> {
    const existingEmployee = await this.findEmployeeOrThrow(employeeId);
    const nextRole = request.role ? normalizeRole(request.role) : existingEmployee.role;
    const nextEmail = request.email?.trim().toLowerCase() ?? existingEmployee.email;
    const nextLoginAllowed = request.loginAllowed ?? existingEmployee.loginAllowed;
    const nextPassword = request.password?.trim();

    if (!existingEmployee.loginAllowed && nextLoginAllowed && !nextPassword) {
      throw new HttpError(400, "password is required when enabling login");
    }

    await this.ensureRelatedEntitiesExist(request);

    const updatedEmployee = await this.prisma.employee.update({
      where: { employeeId: existingEmployee.employeeId },
      data: {
        name: request.name?.trim() ?? existingEmployee.name,
        email: nextEmail,
        password: nextPassword ? await hashPassword(nextPassword) : existingEmployee.password,
        profilePictureUrl: normalizeOptionalString(request.profilePictureUrl, existingEmployee.profilePictureUrl),
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
