import type {
  LeaveDocument,
  Attendance,
  AttendanceActivity,
  Department,
  Designation,
  Employee,
  Leave,
  LeaveQuota,
  PrismaClient,
  Prisma
} from "@prisma/client";

import { DurationType, LeaveStatus, LeaveType } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import type {
  AdminLeaveApplyDto,
  AttendancePayloadDto,
  BulkAttendanceRequestDto,
  LeaveApplyDto,
  LeaveStatusUpdateDto,
  MonthAttendanceRequestDto
} from "../modules/attendance/dto.js";

const DEFAULT_LEAVE_TYPES = ["SICK", "CASUAL", "EARNED"] as const;
const DEFAULT_TOTAL = 5;
const DEFAULT_MONTHLY_LIMIT = 2;

type AttendanceWithEmployee = Attendance & {
  employee: Employee;
  markedBy: Employee | null;
};

type AttendanceWithEmployeeDetails = Attendance & {
  employee: Employee & {
    department: Department | null;
    designation: Designation | null;
  };
  markedBy: Employee | null;
};

type LeaveWithPeople = Leave & {
  employee: Employee;
  approvedBy: Employee | null;
  documents: LeaveDocument[];
};

export class AttendanceLeaveService {
  constructor(private readonly prisma: PrismaClient) {}

  async assignDefaultLeaveQuotas(employeeId: string): Promise<void> {
    const year = new Date().getUTCFullYear();

    for (const leaveType of DEFAULT_LEAVE_TYPES) {
      const exists = await this.prisma.leaveQuota.findFirst({
        where: {
          employeeId,
          leaveType,
          year
        },
        select: { id: true }
      });

      if (!exists) {
        await this.prisma.leaveQuota.create({
          data: {
            employeeId,
            leaveType,
            year,
            totalLeaves: DEFAULT_TOTAL,
            monthlyLimit: DEFAULT_MONTHLY_LIMIT,
            totalTaken: 0,
            overUtilized: 0,
            remainingLeaves: DEFAULT_TOTAL
          }
        });
      }
    }
  }

  async clockIn(employeeId: string, payload: AttendancePayloadDto, date?: string | null): Promise<Record<string, unknown>> {
    const targetDate = parseDate(date) ?? startOfDay(new Date());
    const activity = await this.createActivity(
      employeeId,
      targetDate,
      "IN",
      payload.clockInTime,
      payload.clockInLocation ?? null,
      payload.clockInWorkingFrom ?? null
    );

    return { message: "Clock-in recorded", activityId: Number(activity.id) };
  }

  async clockOut(employeeId: string, payload: AttendancePayloadDto, date?: string | null): Promise<Record<string, unknown>> {
    const targetDate = parseDate(date) ?? startOfDay(new Date());
    const activity = await this.createActivity(
      employeeId,
      targetDate,
      "OUT",
      payload.clockOutTime,
      payload.clockOutLocation ?? null,
      payload.clockOutWorkingFrom ?? null
    );

    return { message: "Clock-out recorded", activityId: Number(activity.id) };
  }

  async getMyActivities(employeeId: string, date: string): Promise<Record<string, unknown>[]> {
    const targetDate = requireDate(date, "date is required");
    const activities = await this.prisma.attendanceActivity.findMany({
      where: {
        employeeId,
        date: targetDate
      },
      orderBy: { createdAt: "asc" }
    });

    return activities.map((activity) => ({
      id: Number(activity.id),
      employeeId: activity.employeeId,
      date: activity.date,
      type: activity.type,
      time: activity.time,
      location: activity.location,
      workingFrom: activity.workingFrom,
      attendanceId: activity.attendanceId ? Number(activity.attendanceId) : null,
      createdAt: activity.createdAt
    }));
  }

  async getMyAttendance(employeeId: string): Promise<Record<string, unknown>[]> {
    const attendances = await this.prisma.attendance.findMany({
      where: { employeeId },
      include: {
        employee: true,
        markedBy: true
      },
      orderBy: { date: "desc" }
    });

    return attendances.map((attendance) => mapAttendance(attendance));
  }

  async getAllSavedAttendance(): Promise<Record<string, unknown>[]> {
    const attendances = await this.prisma.attendance.findMany({
      include: {
        employee: true,
        markedBy: true
      },
      orderBy: [{ date: "desc" }, { createdAt: "desc" }]
    });

    return attendances.map((attendance) => mapAttendance(attendance));
  }

  async getAttendanceById(attendanceId: number): Promise<Record<string, unknown>> {
    const attendance = await this.prisma.attendance.findUnique({
      where: { id: BigInt(attendanceId) },
      include: {
        employee: {
          include: {
            department: true,
            designation: true
          }
        },
        markedBy: true
      }
    });

    if (!attendance) {
      throw new HttpError(404, `Attendance not found with id: ${attendanceId}`);
    }

    return mapAttendanceDetailed(attendance);
  }

  async getAllSavedAttendanceForEmployee(employeeId: string): Promise<Record<string, unknown>[]> {
    const normalizedEmployeeId = normalizeEmployeeId(employeeId);
    await this.ensureEmployee(normalizedEmployeeId);

    const attendances = await this.prisma.attendance.findMany({
      where: { employeeId: normalizedEmployeeId },
      include: {
        employee: true,
        markedBy: true
      },
      orderBy: [{ date: "desc" }, { createdAt: "desc" }]
    });

    return attendances.map((attendance) => mapAttendance(attendance));
  }

  async markAttendanceForEmployees(
    request: BulkAttendanceRequestDto,
    fallbackMarkedById?: string
  ): Promise<Record<string, Record<string, string>>> {
    const employeeIds = request.employeeIds?.map(normalizeEmployeeId).filter(Boolean) ?? [];
    const dates = request.dates?.map((date) => requireDate(date, "Invalid date")) ?? [];
    const payload = request.payload;

    if (!employeeIds.length) {
      throw new HttpError(400, "employeeIds cannot be empty");
    }

    if (!dates.length) {
      throw new HttpError(400, "dates cannot be empty");
    }

    if (!payload) {
      throw new HttpError(400, "payload is required");
    }

    const markedById = fallbackMarkedById ?? request.markedBy ?? null;
    const markedByEmployee = markedById
      ? await this.prisma.employee.findUnique({
          where: { employeeId: normalizeEmployeeId(markedById) },
          select: { employeeId: true }
        })
      : null;

    const result: Record<string, Record<string, string>> = {};

    for (const employeeId of employeeIds) {
      await this.ensureEmployee(employeeId);
      result[employeeId] = {};

      for (const date of dates) {
        const status = await this.prisma.$transaction((tx) =>
          markAdminAttendance(tx, employeeId, date, payload, Boolean(request.overwrite), markedByEmployee?.employeeId ?? null)
        );
        result[employeeId][toDateKey(date)] = status;
      }
    }

    return result;
  }

  async markAttendanceForMonth(
    request: MonthAttendanceRequestDto,
    fallbackMarkedById?: string
  ): Promise<Record<string, Record<string, string>>> {
    if (!request.year || !request.month) {
      throw new HttpError(400, "year and month are required");
    }

    const monthStart = new Date(Date.UTC(request.year, request.month - 1, 1));
    const monthEnd = new Date(Date.UTC(request.year, request.month, 0));
    const dates: string[] = [];
    const current = new Date(monthStart);

    while (current <= monthEnd) {
      dates.push(toDateKey(current));
      current.setUTCDate(current.getUTCDate() + 1);
    }

    return this.markAttendanceForEmployees(
      {
        employeeIds: request.employeeIds,
        dates,
        payload: request.payload,
        overwrite: request.overwrite,
        markedBy: request.markedBy
      },
      fallbackMarkedById
    );
  }

  async getAttendanceSummary(employeeId: string, from: string, to: string): Promise<Record<string, unknown>> {
    const fromDate = requireDate(from, "from is required");
    const toDate = requireDate(to, "to is required");
    const attendances = await this.prisma.attendance.findMany({
      where: {
        employeeId,
        date: {
          gte: fromDate,
          lte: toDate
        }
      }
    });

    return {
      employeeId,
      from: fromDate,
      to: toDate,
      totalDays: attendances.length,
      presentDays: attendances.filter((item) => item.isPresent).length,
      lateDays: attendances.filter((item) => item.late).length,
      halfDays: attendances.filter((item) => item.halfDay).length
    };
  }

  async getAttendanceBetween(from: string, to: string, employeeIds: string[]): Promise<Record<string, unknown>[]> {
    const fromDate = requireDate(from, "from is required");
    const toDate = requireDate(to, "to is required");
    const normalizedEmployeeIds = employeeIds.map(normalizeEmployeeId).filter(Boolean);

    if (!normalizedEmployeeIds.length) {
      throw new HttpError(400, "employeeIds cannot be empty");
    }

    const attendances = await this.prisma.attendance.findMany({
      where: {
        employeeId: { in: normalizedEmployeeIds },
        date: {
          gte: fromDate,
          lte: toDate
        }
      },
      include: {
        employee: true,
        markedBy: true
      },
      orderBy: [{ date: "asc" }, { employeeId: "asc" }]
    });

    return attendances.map((attendance) => mapAttendance(attendance));
  }

  async getAttendanceCalendar(employeeId: string, from: string, to: string): Promise<Record<string, Record<string, unknown>>> {
    const normalizedEmployeeId = normalizeEmployeeId(employeeId);
    const fromDate = requireDate(from, "from is required");
    const toDate = requireDate(to, "to is required");

    await this.ensureEmployee(normalizedEmployeeId);

    const attendances = await this.prisma.attendance.findMany({
      where: {
        employeeId: normalizedEmployeeId,
        date: {
          gte: fromDate,
          lte: toDate
        }
      },
      include: {
        employee: true,
        markedBy: true
      }
    });

    const approvedLeaves = await this.prisma.leave.findMany({
      where: {
        employeeId: normalizedEmployeeId,
        status: LeaveStatus.APPROVED,
        OR: [
          {
            singleDate: {
              gte: fromDate,
              lte: toDate
            }
          },
          {
            startDate: { lte: toDate },
            endDate: { gte: fromDate }
          }
        ]
      }
    });

    const attendanceMap = new Map(attendances.map((attendance) => [toDateKey(attendance.date), attendance]));
    const leaveDateKeys = new Set<string>();

    for (const leave of approvedLeaves) {
      for (const dateKey of expandLeaveDateKeys(leave)) {
        leaveDateKeys.add(dateKey);
      }
    }

    const calendar: Record<string, Record<string, unknown>> = {};
    const cursor = new Date(fromDate);

    while (cursor <= toDate) {
      const dateKey = toDateKey(cursor);
      const attendance = attendanceMap.get(dateKey);

      if (attendance) {
        calendar[dateKey] = mapAttendance(attendance as AttendanceWithEmployee);
      } else {
        calendar[dateKey] = {
          date: new Date(cursor),
          employeeId: normalizedEmployeeId,
          status: leaveDateKeys.has(dateKey) ? "LEAVE" : "ABSENT",
          holiday: false,
          leave: leaveDateKeys.has(dateKey),
          isPresent: false
        };
      }

      cursor.setUTCDate(cursor.getUTCDate() + 1);
    }

    return calendar;
  }

  async getWorkFromHomeOnDate(date: string): Promise<Record<string, unknown>[]> {
    const targetDate = requireDate(date, "date is required");

    const attendances = await this.prisma.attendance.findMany({
      where: {
        date: targetDate,
        OR: [
          { clockInWorkingFrom: { contains: "HOME", mode: "insensitive" } },
          { clockOutWorkingFrom: { contains: "HOME", mode: "insensitive" } }
        ]
      },
      include: {
        employee: {
          include: {
            department: true,
            designation: true
          }
        },
        markedBy: true
      },
      orderBy: { employeeId: "asc" }
    });

    return attendances.map((attendance) => mapAttendanceDetailed(attendance));
  }

  async getWorkFromHomeBetween(from: string, to: string): Promise<Record<string, unknown>[]> {
    const fromDate = requireDate(from, "from is required");
    const toDate = requireDate(to, "to is required");

    const attendances = await this.prisma.attendance.findMany({
      where: {
        date: {
          gte: fromDate,
          lte: toDate
        },
        OR: [
          { clockInWorkingFrom: { contains: "HOME", mode: "insensitive" } },
          { clockOutWorkingFrom: { contains: "HOME", mode: "insensitive" } }
        ]
      },
      include: {
        employee: {
          include: {
            department: true,
            designation: true
          }
        },
        markedBy: true
      },
      orderBy: [{ date: "asc" }, { employeeId: "asc" }]
    });

    return attendances.map((attendance) => mapAttendanceDetailed(attendance));
  }

  async attendanceExists(employeeId: string, date: string): Promise<boolean> {
    const normalizedEmployeeId = normalizeEmployeeId(employeeId);
    const targetDate = requireDate(date, "date is required");

    const attendance = await this.prisma.attendance.findFirst({
      where: {
        employeeId: normalizedEmployeeId,
        date: targetDate
      },
      select: { id: true }
    });

    return Boolean(attendance);
  }

  async deleteAttendance(employeeId: string, date: string): Promise<void> {
    const normalizedEmployeeId = normalizeEmployeeId(employeeId);
    const targetDate = requireDate(date, "date is required");

    await this.prisma.$transaction(async (tx) => {
      const attendance = await tx.attendance.findFirst({
        where: {
          employeeId: normalizedEmployeeId,
          date: targetDate
        }
      });

      if (!attendance) {
        throw new HttpError(404, "Attendance not found");
      }

      await tx.attendanceActivity.deleteMany({
        where: {
          attendanceId: attendance.id
        }
      });

      await tx.attendance.delete({
        where: { id: attendance.id }
      });
    });
  }

  async editAttendanceForEmployeeDate(
    employeeId: string,
    date: string,
    payload: AttendancePayloadDto,
    overwrite = true,
    markedById?: string | null
  ): Promise<Record<string, unknown>> {
    const normalizedEmployeeId = normalizeEmployeeId(employeeId);
    const targetDate = requireDate(date, "date is required");
    await this.ensureEmployee(normalizedEmployeeId);

    const markedByEmployee = markedById
      ? await this.prisma.employee.findUnique({
          where: { employeeId: normalizeEmployeeId(markedById) },
          select: { employeeId: true }
        })
      : null;

    await this.prisma.$transaction((tx) =>
      markAdminAttendance(tx, normalizedEmployeeId, targetDate, payload, overwrite, markedByEmployee?.employeeId ?? null)
    );

    return { message: "Attendance updated" };
  }

  async applyLeave(employeeId: string, request: LeaveApplyDto): Promise<Record<string, unknown>> {
    const employee = await this.ensureEmployee(employeeId);
    const leaveType = parseLeaveType(request.leaveType);
    const durationType = parseDurationType(request.durationType);
    validateLeaveRequest(durationType, request);

    const leaveDays = calculateLeaveDays(durationType, request);
    const quota = await this.ensureQuota(employee.employeeId, leaveType);
    const isPaid = quota.remainingLeaves < leaveDays;

    const savedLeave = await this.prisma.leave.create({
      data: {
        employeeId: employee.employeeId,
        leaveType,
        durationType,
        startDate: parseDate(request.startDate),
        endDate: parseDate(request.endDate),
        singleDate: parseDate(request.singleDate),
        reason: request.reason?.trim() || null,
        status: LeaveStatus.PENDING,
        isPaid,
        documents: request.documentUrls?.length
          ? {
              create: request.documentUrls
                .map((url, index) => url.trim())
                .filter(Boolean)
                .map((url, index) => ({
                  filename: `leave-document-${index + 1}`,
                  url
                }))
            }
          : undefined
      },
      include: {
        employee: true,
        approvedBy: true,
        documents: true
      }
    });

    return mapLeave(savedLeave);
  }

  async applyLeavesForEmployees(request: AdminLeaveApplyDto, adminId: string): Promise<Record<string, unknown>[]> {
    const employeeIds = request.employeeIds?.map(normalizeEmployeeId).filter(Boolean) ?? [];

    if (!employeeIds.length) {
      throw new HttpError(400, "employeeIds cannot be empty");
    }

    const status = request.status ? parseLeaveStatus(request.status) : LeaveStatus.PENDING;
    const results: Record<string, unknown>[] = [];

    for (const employeeId of employeeIds) {
      const employee = await this.ensureEmployee(employeeId);
      const leaveType = parseLeaveType(request.leaveType);
      const durationType = parseDurationType(request.durationType);
      validateLeaveRequest(durationType, request);
      const leaveDays = calculateLeaveDays(durationType, request);
      const quota = await this.ensureQuota(employee.employeeId, leaveType);
      const isPaid = quota.remainingLeaves < leaveDays;

      const adminEmployee = await this.prisma.employee.findUnique({
        where: { employeeId: normalizeEmployeeId(adminId) },
        select: { employeeId: true }
      });

      const savedLeave = await this.prisma.$transaction(async (tx) => {
        const leave = await tx.leave.create({
          data: {
            employeeId: employee.employeeId,
            leaveType,
            durationType,
            startDate: parseDate(request.startDate),
            endDate: parseDate(request.endDate),
            singleDate: parseDate(request.singleDate),
            reason: request.reason?.trim() || null,
            status,
            isPaid,
            approvedById: status === LeaveStatus.APPROVED ? adminEmployee?.employeeId ?? null : null,
            approvedAt: status === LeaveStatus.APPROVED ? new Date() : null,
            rejectedAt: status === LeaveStatus.REJECTED ? new Date() : null,
            documents: request.documentUrls?.length
              ? {
                  create: request.documentUrls
                    .map((url) => url.trim())
                    .filter(Boolean)
                    .map((url, index) => ({
                      filename: `leave-document-${index + 1}`,
                      url
                    }))
                }
              : undefined
          },
          include: { employee: true, approvedBy: true, documents: true }
        });

        if (status === LeaveStatus.APPROVED) {
          await applyQuotaConsumption(tx, leave);
        }

        return leave;
      });

      results.push(mapLeave(savedLeave));
    }

    return results;
  }

  async getMyLeaves(employeeId: string): Promise<Record<string, unknown>[]> {
    const leaves = await this.prisma.leave.findMany({
      where: { employeeId },
      include: { employee: true, approvedBy: true, documents: true },
      orderBy: { createdAt: "desc" }
    });

    return leaves.map(mapLeave);
  }

  async getAllLeaves(): Promise<Record<string, unknown>[]> {
    const leaves = await this.prisma.leave.findMany({
      include: { employee: true, approvedBy: true, documents: true },
      orderBy: { createdAt: "desc" }
    });

    return leaves.map(mapLeave);
  }

  async getPendingLeaves(): Promise<Record<string, unknown>[]> {
    const leaves = await this.prisma.leave.findMany({
      where: { status: LeaveStatus.PENDING },
      include: { employee: true, approvedBy: true, documents: true },
      orderBy: { createdAt: "desc" }
    });

    return leaves.map(mapLeave);
  }

  async updateLeaveStatus(leaveId: number, adminId: string, dto: LeaveStatusUpdateDto): Promise<Record<string, unknown>> {
    const leave = await this.prisma.leave.findUnique({
      where: { id: BigInt(leaveId) },
      include: { employee: true, approvedBy: true, documents: true }
    });

    if (!leave) {
      throw new HttpError(404, "Leave not found");
    }

    const nextStatus = parseLeaveStatus(dto.status);

    if (nextStatus === LeaveStatus.REJECTED && !dto.rejectionReason?.trim()) {
      throw new HttpError(400, "Rejection reason is required when rejecting leave");
    }

    const adminEmployee = await this.prisma.employee.findUnique({
      where: { employeeId: adminId },
      select: { employeeId: true }
    });

    const updatedLeave = await this.prisma.$transaction(async (tx) => {
      if (leave.status !== LeaveStatus.APPROVED && nextStatus === LeaveStatus.APPROVED) {
        await applyQuotaConsumption(tx, leave);
      }

      if (leave.status === LeaveStatus.APPROVED && nextStatus !== LeaveStatus.APPROVED) {
        await restoreQuotaConsumption(tx, leave);
      }

      return tx.leave.update({
        where: { id: leave.id },
        data: {
          status: nextStatus,
          rejectionReason: nextStatus === LeaveStatus.REJECTED ? dto.rejectionReason?.trim() || null : null,
          approvedById: nextStatus === LeaveStatus.APPROVED ? adminEmployee?.employeeId ?? null : null,
          approvedAt: nextStatus === LeaveStatus.APPROVED ? new Date() : null,
          rejectedAt: nextStatus === LeaveStatus.REJECTED ? new Date() : null
        },
        include: { employee: true, approvedBy: true, documents: true }
      });
    });

    return mapLeave(updatedLeave);
  }

  async deleteLeave(leaveId: number, employeeId: string, isAdmin: boolean): Promise<void> {
    const leave = await this.prisma.leave.findUnique({
      where: { id: BigInt(leaveId) }
    });

    if (!leave) {
      throw new HttpError(404, "Leave not found");
    }

    if (leave.employeeId !== employeeId && !isAdmin) {
      throw new HttpError(403, "Not authorized to delete this leave");
    }

    await this.prisma.$transaction(async (tx) => {
      if (leave.status === LeaveStatus.APPROVED) {
        await restoreQuotaConsumption(tx, leave);
      }

      await tx.leave.delete({
        where: { id: leave.id }
      });
    });
  }

  async deleteLeaveDocument(leaveId: number, documentId: number): Promise<void> {
    const leave = await this.prisma.leave.findUnique({
      where: { id: BigInt(leaveId) },
      select: { id: true }
    });

    if (!leave) {
      throw new HttpError(404, "Leave not found");
    }

    const document = await this.prisma.leaveDocument.findFirst({
      where: {
        id: BigInt(documentId),
        leaveId: BigInt(leaveId)
      },
      select: { id: true }
    });

    if (!document) {
      throw new HttpError(404, "Leave document not found");
    }

    await this.prisma.leaveDocument.delete({
      where: { id: BigInt(documentId) }
    });
  }

  async getLeavesForEmployee(employeeId: string): Promise<Record<string, unknown>[]> {
    await this.ensureEmployee(employeeId);
    return this.getMyLeaves(employeeId);
  }

  async getLeaveById(leaveId: number): Promise<Record<string, unknown>> {
    const leave = await this.prisma.leave.findUnique({
      where: { id: BigInt(leaveId) },
      include: { employee: true, approvedBy: true, documents: true }
    });

    if (!leave) {
      throw new HttpError(404, "Leave not found");
    }

    return mapLeave(leave);
  }

  async getLeaveCalendar(date: string): Promise<Record<string, unknown>[]> {
    const targetDate = requireDate(date, "date is required");
    const leaves = await this.prisma.leave.findMany({
      where: {
        status: LeaveStatus.APPROVED,
        OR: [
          { singleDate: targetDate },
          {
            startDate: { lte: targetDate },
            endDate: { gte: targetDate }
          }
        ]
      },
      include: { employee: true }
    });

    return [
      {
        date: targetDate,
        employeesOnLeave: leaves.map((leave) => ({
          employeeId: leave.employee.employeeId,
          employeeName: leave.employee.name,
          department: null,
          leaveType: leave.leaveType
        }))
      }
    ];
  }

  async getQuotasForEmployee(employeeId: string): Promise<Record<string, unknown>[]> {
    const currentYear = new Date().getUTCFullYear();
    const quotas = await this.prisma.leaveQuota.findMany({
      where: { employeeId, year: currentYear },
      orderBy: { leaveType: "asc" }
    });

    return quotas.map(mapQuota);
  }

  async getQuotasByEmployeeId(employeeId: string): Promise<Record<string, unknown>[]> {
    return this.getQuotasForEmployee(employeeId);
  }

  private async createActivity(
    employeeId: string,
    date: Date,
    type: "IN" | "OUT",
    requestedTime?: string | null,
    location?: string | null,
    workingFrom?: string | null
  ): Promise<AttendanceActivity> {
    await this.ensureEmployee(employeeId);
    const activityTime = parseTime(requestedTime) ?? new Date();

    return this.prisma.$transaction(async (tx) => {
      let attendance = await tx.attendance.findFirst({
        where: { employeeId, date }
      });

      if (attendance?.markedById) {
        throw new HttpError(400, "Attendance for this date was marked by admin");
      }

      const activities = await tx.attendanceActivity.findMany({
        where: { employeeId, date },
        orderBy: { createdAt: "asc" }
      });

      const hasIn = activities.some((item) => item.type === "IN");
      const hasOut = activities.some((item) => item.type === "OUT");

      if (type === "IN" && hasIn) {
        throw new HttpError(400, "You have already clocked in for this date");
      }

      if (type === "OUT" && !hasIn) {
        throw new HttpError(400, "Cannot clock out before clocking in");
      }

      if (type === "OUT" && hasOut) {
        throw new HttpError(400, "You have already clocked out for this date");
      }

      if (!attendance) {
        attendance = await tx.attendance.create({
          data: {
            employeeId,
            date,
            isPresent: true,
            overwritten: false
          }
        });
      }

      const activity = await tx.attendanceActivity.create({
        data: {
          employeeId,
          date,
          type,
          time: activityTime,
          location,
          workingFrom,
          attendanceId: attendance.id
        }
      });

      await recomputeAttendanceSummary(tx, employeeId, date, attendance.id);

      return activity;
    });
  }

  private async ensureEmployee(employeeId: string): Promise<Employee> {
    const normalizedEmployeeId = normalizeEmployeeId(employeeId);
    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: normalizedEmployeeId }
    });

    if (!employee) {
      throw new HttpError(404, "Employee not found");
    }

    return employee;
  }

  private async ensureQuota(employeeId: string, leaveType: LeaveType): Promise<LeaveQuota> {
    const currentYear = new Date().getUTCFullYear();
    let quota = await this.prisma.leaveQuota.findFirst({
      where: {
        employeeId,
        leaveType,
        year: currentYear
      }
    });

    if (!quota) {
      await this.assignDefaultLeaveQuotas(employeeId);
      quota = await this.prisma.leaveQuota.findFirst({
        where: {
          employeeId,
          leaveType,
          year: currentYear
        }
      });
    }

    if (!quota) {
      throw new HttpError(500, "Leave quota not initialized");
    }

    return quota;
  }
}

async function markAdminAttendance(
  prisma: Prisma.TransactionClient,
  employeeId: string,
  date: Date,
  payload: AttendancePayloadDto,
  overwrite: boolean,
  markedById: string | null
): Promise<string> {
  const existingAttendance = await prisma.attendance.findFirst({
    where: {
      employeeId,
      date
    }
  });

  const attendance = existingAttendance
    ? await prisma.attendance.update({
        where: { id: existingAttendance.id },
        data: {
          clockInTime: parseTime(payload.clockInTime),
          clockInLocation: payload.clockInLocation?.trim() || null,
          clockInWorkingFrom: payload.clockInWorkingFrom?.trim() || null,
          clockOutTime: parseTime(payload.clockOutTime),
          clockOutLocation: payload.clockOutLocation?.trim() || null,
          clockOutWorkingFrom: payload.clockOutWorkingFrom?.trim() || null,
          late: payload.late ?? false,
          halfDay: payload.halfDay ?? false,
          overwritten: overwrite,
          isPresent: true,
          markedById
        }
      })
    : await prisma.attendance.create({
        data: {
          employeeId,
          date,
          clockInTime: parseTime(payload.clockInTime),
          clockInLocation: payload.clockInLocation?.trim() || null,
          clockInWorkingFrom: payload.clockInWorkingFrom?.trim() || null,
          clockOutTime: parseTime(payload.clockOutTime),
          clockOutLocation: payload.clockOutLocation?.trim() || null,
          clockOutWorkingFrom: payload.clockOutWorkingFrom?.trim() || null,
          late: payload.late ?? false,
          halfDay: payload.halfDay ?? false,
          overwritten: overwrite,
          isPresent: true,
          markedById
        }
      });

  await prisma.attendanceActivity.deleteMany({
    where: {
      attendanceId: attendance.id
    }
  });

  return "PRESENT";
}

async function recomputeAttendanceSummary(
  prisma: Prisma.TransactionClient,
  employeeId: string,
  date: Date,
  attendanceId: bigint
): Promise<void> {
  const activities = await prisma.attendanceActivity.findMany({
    where: { employeeId, date },
    orderBy: { createdAt: "asc" }
  });

  const firstIn = activities.filter((item) => item.type === "IN").sort((a, b) => a.time.getTime() - b.time.getTime())[0] ?? null;
  const lastOut = activities.filter((item) => item.type === "OUT").sort((a, b) => b.time.getTime() - a.time.getTime())[0] ?? null;
  const lateThreshold = parseTime("09:30:00")!;
  const halfDay = firstIn && lastOut ? lastOut.time.getTime() - firstIn.time.getTime() < 4 * 60 * 60 * 1000 : false;

  await prisma.attendance.update({
    where: { id: attendanceId },
    data: {
      clockInTime: firstIn?.time ?? null,
      clockInLocation: firstIn?.location ?? null,
      clockInWorkingFrom: firstIn?.workingFrom ?? null,
      clockOutTime: lastOut?.time ?? null,
      clockOutLocation: lastOut?.location ?? null,
      clockOutWorkingFrom: lastOut?.workingFrom ?? null,
      isPresent: Boolean(firstIn || lastOut),
      late: firstIn ? firstIn.time.getTime() > lateThreshold.getTime() : false,
      halfDay
    }
  });
}

async function applyQuotaConsumption(prisma: Prisma.TransactionClient, leave: Leave): Promise<void> {
  const quota = await prisma.leaveQuota.findFirst({
    where: {
      employeeId: leave.employeeId,
      leaveType: leave.leaveType,
      year: leave.createdAt.getUTCFullYear()
    }
  });

  if (!quota) {
    return;
  }

  const days = countLeaveDays(leave);
  const totalTaken = quota.totalTaken + days;

  await prisma.leaveQuota.update({
    where: { id: quota.id },
    data: {
      totalTaken,
      remainingLeaves: Math.max(0, quota.totalLeaves - totalTaken),
      overUtilized: Math.max(0, totalTaken - quota.totalLeaves)
    }
  });
}

async function restoreQuotaConsumption(prisma: Prisma.TransactionClient, leave: Leave): Promise<void> {
  const quota = await prisma.leaveQuota.findFirst({
    where: {
      employeeId: leave.employeeId,
      leaveType: leave.leaveType,
      year: leave.createdAt.getUTCFullYear()
    }
  });

  if (!quota) {
    return;
  }

  const days = countLeaveDays(leave);
  const totalTaken = Math.max(0, quota.totalTaken - days);

  await prisma.leaveQuota.update({
    where: { id: quota.id },
    data: {
      totalTaken,
      remainingLeaves: Math.max(0, quota.totalLeaves - totalTaken),
      overUtilized: Math.max(0, totalTaken - quota.totalLeaves)
    }
  });
}

function mapAttendance(attendance: AttendanceWithEmployee): Record<string, unknown> {
  return {
    date: attendance.date,
    employeeId: attendance.employee.employeeId,
    employeeName: attendance.employee.name,
    status: attendance.isPresent ? "PRESENT" : "ABSENT",
    attendanceId: Number(attendance.id),
    overwritten: attendance.overwritten,
    late: attendance.late,
    halfDay: attendance.halfDay,
    clockInTime: attendance.clockInTime,
    clockInLocation: attendance.clockInLocation,
    clockInWorkingFrom: attendance.clockInWorkingFrom,
    clockOutTime: attendance.clockOutTime,
    clockOutLocation: attendance.clockOutLocation,
    clockOutWorkingFrom: attendance.clockOutWorkingFrom,
    markedById: attendance.markedBy?.employeeId ?? null,
    markedByName: attendance.markedBy?.name ?? null,
    holiday: false,
    leave: false,
    isPresent: attendance.isPresent
  };
}

function mapAttendanceDetailed(attendance: AttendanceWithEmployeeDetails): Record<string, unknown> {
  return {
    ...mapAttendance(attendance),
    profilePictureUrl: attendance.employee.profilePictureUrl ?? null,
    departmentName: attendance.employee.department?.departmentName ?? null,
    designationName: attendance.employee.designation?.designationName ?? null
  };
}

function mapLeave(leave: LeaveWithPeople): Record<string, unknown> {
  return {
    id: Number(leave.id),
    employeeId: leave.employee.employeeId,
    employeeName: leave.employee.name,
    leaveType: leave.leaveType,
    durationType: leave.durationType,
    startDate: leave.startDate,
    endDate: leave.endDate,
    singleDate: leave.singleDate,
    reason: leave.reason,
    status: leave.status,
    rejectionReason: leave.rejectionReason,
    approvedByName: leave.approvedBy?.name ?? null,
    isPaid: leave.isPaid,
    approvedAt: leave.approvedAt,
    rejectedAt: leave.rejectedAt,
    documentUrls: leave.documents.map((document) => document.url),
    createdAt: leave.createdAt,
    updatedAt: leave.updatedAt
  };
}

function mapQuota(quota: LeaveQuota): Record<string, unknown> {
  return {
    id: Number(quota.id),
    leaveType: quota.leaveType,
    totalLeaves: quota.totalLeaves,
    monthlyLimit: quota.monthlyLimit,
    totalTaken: quota.totalTaken,
    overUtilized: quota.overUtilized,
    remainingLeaves: quota.remainingLeaves
  };
}

function validateLeaveRequest(durationType: DurationType, request: LeaveApplyDto): void {
  if (!request.leaveType || !request.durationType) {
    throw new HttpError(400, "leaveType and durationType are required");
  }

  if (durationType === DurationType.MULTIPLE) {
    if (!request.startDate || !request.endDate) {
      throw new HttpError(400, "startDate and endDate are required for MULTIPLE leaves");
    }
    return;
  }

  if (!request.singleDate) {
    throw new HttpError(400, "singleDate is required for single-day leaves");
  }
}

function calculateLeaveDays(durationType: DurationType, request: LeaveApplyDto): number {
  if (durationType === DurationType.MULTIPLE) {
    const startDate = requireDate(request.startDate, "startDate is required");
    const endDate = requireDate(request.endDate, "endDate is required");
    return Math.floor((endDate.getTime() - startDate.getTime()) / 86400000) + 1;
  }

  return 1;
}

function countLeaveDays(leave: Leave): number {
  if (leave.durationType === DurationType.MULTIPLE && leave.startDate && leave.endDate) {
    return Math.floor((leave.endDate.getTime() - leave.startDate.getTime()) / 86400000) + 1;
  }

  return 1;
}

function parseLeaveType(value?: string): LeaveType {
  if (!value) {
    throw new HttpError(400, "leaveType is required");
  }

  return LeaveType[value as keyof typeof LeaveType];
}

function parseDurationType(value?: string): DurationType {
  if (!value) {
    throw new HttpError(400, "durationType is required");
  }

  return DurationType[value as keyof typeof DurationType];
}

function parseLeaveStatus(value?: string): LeaveStatus {
  if (!value) {
    throw new HttpError(400, "status is required");
  }

  return LeaveStatus[value as keyof typeof LeaveStatus];
}

function requireDate(value: string | null | undefined, message: string): Date {
  const parsed = parseDate(value);

  if (!parsed) {
    throw new HttpError(400, message);
  }

  return parsed;
}

function parseDate(value?: string | null): Date | null {
  if (!value) {
    return null;
  }

  return startOfDay(new Date(value));
}

function parseTime(value?: string | null): Date | null {
  if (!value) {
    return null;
  }

  const parts = value.split(":");
  const hour = Number(parts[0] ?? 0);
  const minute = Number(parts[1] ?? 0);
  const second = Number(parts[2] ?? 0);
  const date = new Date();
  date.setHours(hour, minute, second, 0);
  return date;
}

function startOfDay(date: Date): Date {
  const copy = new Date(date);
  copy.setUTCHours(0, 0, 0, 0);
  return copy;
}

function normalizeEmployeeId(employeeId: string): string {
  return employeeId.trim().toUpperCase();
}

function toDateKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function expandLeaveDateKeys(leave: Leave): string[] {
  if (leave.singleDate) {
    return [toDateKey(leave.singleDate)];
  }

  if (leave.startDate && leave.endDate) {
    const keys: string[] = [];
    const cursor = new Date(leave.startDate);

    while (cursor <= leave.endDate) {
      keys.push(toDateKey(cursor));
      cursor.setUTCDate(cursor.getUTCDate() + 1);
    }

    return keys;
  }

  return [];
}
