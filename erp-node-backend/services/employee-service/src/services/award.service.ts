import type { Award, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import { MediaStorageService } from "./media-storage.service.js";

export class AwardService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly mediaStorageService: MediaStorageService
  ) {}

  async createAward(
    input: {
      title?: string;
      summary?: string | null;
      iconUrl?: string | null;
      iconFile?: { filename: string | null; contentType: string | null; data: Buffer } | null;
    },
    uploadedByEmployeeId: string
  ): Promise<Record<string, unknown>> {
    if (!input.title?.trim()) {
      throw new HttpError(400, "Award title is required");
    }

    const iconUpload = await this.mediaStorageService.saveUploadedFile(input.iconFile, "awards/icons");
    const award = await this.prisma.award.create({
      data: {
        title: input.title.trim(),
        summary: input.summary?.trim() || null,
        uploadedByEmployeeId: uploadedByEmployeeId.trim().toUpperCase(),
        iconUrl: iconUpload?.url ?? (input.iconUrl?.trim() || null),
        iconObjectKey: iconUpload?.objectKey ?? null,
        isActive: true
      }
    });

    return mapAward(award);
  }

  async updateAward(
    awardId: number,
    input: {
      title?: string;
      summary?: string | null;
      iconUrl?: string | null;
      iconFile?: { filename: string | null; contentType: string | null; data: Buffer } | null;
    },
    uploadedByEmployeeId: string
  ): Promise<Record<string, unknown>> {
    const existing = await this.findAward(awardId);
    const iconUpload = await this.mediaStorageService.saveUploadedFile(input.iconFile, "awards/icons");

    const updated = await this.prisma.award.update({
      where: { id: BigInt(awardId) },
      data: {
        title: input.title?.trim() || existing.title,
        summary: input.summary !== undefined ? input.summary?.trim() || null : existing.summary,
        uploadedByEmployeeId: uploadedByEmployeeId.trim().toUpperCase(),
        iconUrl: iconUpload?.url ?? (input.iconUrl !== undefined ? input.iconUrl?.trim() || null : existing.iconUrl),
        iconObjectKey: iconUpload?.objectKey ?? existing.iconObjectKey
      }
    });

    return mapAward(updated);
  }

  async getAllAwards(): Promise<Record<string, unknown>[]> {
    const awards = await this.prisma.award.findMany({
      orderBy: { createdAt: "desc" }
    });

    return awards.map(mapAward);
  }

  async getActiveAwards(): Promise<Record<string, unknown>[]> {
    const awards = await this.prisma.award.findMany({
      where: { isActive: true },
      orderBy: { createdAt: "desc" }
    });

    return awards.map(mapAward);
  }

  async getAwardById(awardId: number, isAdmin: boolean): Promise<Record<string, unknown>> {
    const award = await this.findAward(awardId);

    if (!isAdmin && !award.isActive) {
      throw new HttpError(403, "Access denied to inactive award");
    }

    return mapAward(award);
  }

  async deleteAward(awardId: number): Promise<void> {
    await this.findAward(awardId);
    await this.prisma.award.delete({
      where: { id: BigInt(awardId) }
    });
  }

  async toggleAwardStatus(awardId: number): Promise<Record<string, unknown>> {
    const award = await this.findAward(awardId);
    const updated = await this.prisma.award.update({
      where: { id: award.id },
      data: { isActive: !award.isActive }
    });

    return mapAward(updated);
  }

  private async findAward(awardId: number): Promise<Award> {
    const award = await this.prisma.award.findUnique({
      where: { id: BigInt(awardId) }
    });

    if (!award) {
      throw new HttpError(404, `Award not found with ID: ${awardId}`);
    }

    return award;
  }
}

function mapAward(award: Award): Record<string, unknown> {
  return {
    id: Number(award.id),
    title: award.title,
    summary: award.summary,
    iconUrl: award.iconUrl,
    iconFileId: null,
    isActive: award.isActive,
    createdAt: award.createdAt,
    updatedAt: award.updatedAt
  };
}
