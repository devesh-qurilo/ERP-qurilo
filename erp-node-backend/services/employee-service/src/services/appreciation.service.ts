import type { Appreciation, Award, Employee, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import { MediaStorageService } from "./media-storage.service.js";
import { NotificationService } from "./notification.service.js";

type AppreciationWithRelations = Appreciation & {
  award: Award;
  givenTo: Employee;
};

export class AppreciationService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly mediaStorageService: MediaStorageService,
    private readonly notificationService: NotificationService
  ) {}

  async create(
    adminEmployeeId: string,
    input: {
      awardId?: number;
      givenToEmployeeId?: string;
      date?: string;
      summary?: string | null;
      photoUrl?: string | null;
      photoFile?: { filename: string | null; contentType: string | null; data: Buffer } | null;
    }
  ): Promise<Record<string, unknown>> {
    const awardId = Number(input.awardId);
    const givenToEmployeeId = input.givenToEmployeeId?.trim().toUpperCase();

    if (!awardId || !givenToEmployeeId || !input.date) {
      throw new HttpError(400, "awardId, givenToEmployeeId and date are required");
    }

    const [award, employee] = await Promise.all([
      this.prisma.award.findUnique({ where: { id: BigInt(awardId) } }),
      this.prisma.employee.findUnique({ where: { employeeId: givenToEmployeeId } })
    ]);

    if (!award) {
      throw new HttpError(404, "Award not found");
    }

    if (!employee) {
      throw new HttpError(404, "Employee not found");
    }

    const photoUpload = await this.mediaStorageService.saveUploadedFile(input.photoFile, `appreciations/${employee.employeeId}`);
    const appreciation = await this.prisma.appreciation.create({
      data: {
        awardId: award.id,
        givenToEmployeeId: employee.employeeId,
        date: requireDate(input.date, "date is required"),
        summary: input.summary?.trim() || null,
        photoUrl: photoUpload?.url ?? (input.photoUrl?.trim() || null),
        photoObjectKey: photoUpload?.objectKey ?? null,
        isActive: true
      },
      include: { award: true, givenTo: true }
    });

    await this.notificationService.sendNotification(adminEmployeeId, {
      receiverEmployeeId: employee.employeeId,
      title: "You received an appreciation!",
      message: `You were appreciated for ${award.title}${input.summary?.trim() ? `: ${input.summary.trim()}` : ""}`,
      type: "APPRECIATION"
    });

    return mapAppreciation(appreciation);
  }

  async getAll(): Promise<Record<string, unknown>[]> {
    const items = await this.prisma.appreciation.findMany({
      include: { award: true, givenTo: true },
      orderBy: [{ date: "desc" }, { createdAt: "desc" }]
    });

    return items.map(mapAppreciation);
  }

  async getForEmployee(employeeId: string): Promise<Record<string, unknown>[]> {
    const items = await this.prisma.appreciation.findMany({
      where: { givenToEmployeeId: employeeId.trim().toUpperCase() },
      include: { award: true, givenTo: true },
      orderBy: [{ date: "desc" }, { createdAt: "desc" }]
    });

    return items.map(mapAppreciation);
  }

  async getById(id: number): Promise<Record<string, unknown>> {
    return mapAppreciation(await this.findAppreciation(id));
  }

  async update(
    _adminEmployeeId: string,
    id: number,
    input: {
      awardId?: number;
      givenToEmployeeId?: string;
      date?: string;
      summary?: string | null;
      photoUrl?: string | null;
      photoFile?: { filename: string | null; contentType: string | null; data: Buffer } | null;
    }
  ): Promise<Record<string, unknown>> {
    const existing = await this.findAppreciation(id);
    const nextAwardId = input.awardId ? BigInt(input.awardId) : existing.awardId;
    const nextEmployeeId = input.givenToEmployeeId?.trim().toUpperCase() || existing.givenToEmployeeId;

    const [award, employee] = await Promise.all([
      this.prisma.award.findUnique({ where: { id: nextAwardId } }),
      this.prisma.employee.findUnique({ where: { employeeId: nextEmployeeId } })
    ]);

    if (!award) {
      throw new HttpError(404, "Award not found");
    }

    if (!employee) {
      throw new HttpError(404, "Employee not found");
    }

    const photoUpload = await this.mediaStorageService.saveUploadedFile(input.photoFile, `appreciations/${employee.employeeId}`);
    const updated = await this.prisma.appreciation.update({
      where: { id: BigInt(id) },
      data: {
        awardId: award.id,
        givenToEmployeeId: employee.employeeId,
        date: input.date ? requireDate(input.date, "date is required") : existing.date,
        summary: input.summary !== undefined ? input.summary?.trim() || null : existing.summary,
        photoUrl: photoUpload?.url ?? (input.photoUrl !== undefined ? input.photoUrl?.trim() || null : existing.photoUrl),
        photoObjectKey: photoUpload?.objectKey ?? existing.photoObjectKey,
        isActive: true
      },
      include: { award: true, givenTo: true }
    });

    return mapAppreciation(updated);
  }

  async delete(_adminEmployeeId: string, id: number): Promise<void> {
    await this.findAppreciation(id);
    await this.prisma.appreciation.delete({
      where: { id: BigInt(id) }
    });
  }

  private async findAppreciation(id: number): Promise<AppreciationWithRelations> {
    const appreciation = await this.prisma.appreciation.findUnique({
      where: { id: BigInt(id) },
      include: { award: true, givenTo: true }
    });

    if (!appreciation) {
      throw new HttpError(404, "Appreciation not found");
    }

    return appreciation;
  }
}

function mapAppreciation(appreciation: AppreciationWithRelations): Record<string, unknown> {
  return {
    id: Number(appreciation.id),
    awardId: Number(appreciation.awardId),
    awardTitle: appreciation.award.title,
    givenToEmployeeId: appreciation.givenTo.employeeId,
    givenToEmployeeName: appreciation.givenTo.name,
    date: appreciation.date,
    summary: appreciation.summary,
    photoUrl: appreciation.photoUrl,
    photoFileId: null,
    isActive: appreciation.isActive,
    createdAt: appreciation.createdAt,
    updatedAt: appreciation.updatedAt
  };
}

function requireDate(value: string, message: string): Date {
  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    throw new HttpError(400, message);
  }

  parsed.setUTCHours(0, 0, 0, 0);
  return parsed;
}
