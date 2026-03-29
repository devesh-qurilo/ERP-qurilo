import type { Company, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import type { CompanyRequestDto } from "../modules/company/dto.js";
import type { MediaStorageService } from "./media-storage.service.js";

export class CompanyService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly mediaStorageService: MediaStorageService
  ) {}

  async createCompany(
    _adminEmployeeId: string,
    dto: CompanyRequestDto,
    logoFile?: { filename: string | null; contentType: string | null; data: Buffer } | null
  ): Promise<Record<string, unknown>> {
    const companyName = dto.companyName?.trim();
    const email = dto.email?.trim().toLowerCase();
    const contactNo = dto.contactNo?.trim();

    if (!companyName || !email || !contactNo) {
      throw new HttpError(400, "companyName, email and contactNo are required");
    }

    validateContactNo(contactNo);

    const existingCompany = await this.prisma.company.findFirst({
      where: { isActive: true }
    });

    if (existingCompany) {
      throw new HttpError(409, "Company already exists. Use update instead.");
    }

    await this.ensureCompanyUnique(companyName, email);

    const uploadedLogo = await this.mediaStorageService.saveUploadedFile(logoFile, "company/logo");
    const company = await this.prisma.company.create({
      data: {
        companyName,
        email,
        contactNo,
        website: dto.website?.trim() || null,
        address: dto.address?.trim() || null,
        logoUrl: uploadedLogo?.url ?? (dto.logoUrl?.trim() || null),
        isActive: true
      }
    });

    return mapCompany(company);
  }

  async getCompany(): Promise<Record<string, unknown>> {
    const company = await this.prisma.company.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: "desc" }
    });

    if (!company) {
      throw new HttpError(404, "Company not found");
    }

    return mapCompany(company);
  }

  async updateCompany(
    _adminEmployeeId: string,
    dto: CompanyRequestDto,
    logoFile?: { filename: string | null; contentType: string | null; data: Buffer } | null
  ): Promise<Record<string, unknown>> {
    const company = await this.prisma.company.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: "desc" }
    });

    if (!company) {
      throw new HttpError(404, "Company not found");
    }

    const nextCompanyName = dto.companyName?.trim() ?? company.companyName;
    const nextEmail = dto.email?.trim().toLowerCase() ?? company.email;
    const nextContactNo = dto.contactNo?.trim() ?? company.contactNo;

    validateContactNo(nextContactNo);
    await this.ensureCompanyUnique(nextCompanyName, nextEmail, Number(company.id));

    const uploadedLogo = await this.mediaStorageService.saveUploadedFile(logoFile, "company/logo");
    const updatedCompany = await this.prisma.company.update({
      where: { id: company.id },
      data: {
        companyName: nextCompanyName,
        email: nextEmail,
        contactNo: nextContactNo,
        website: dto.website !== undefined ? dto.website?.trim() || null : company.website,
        address: dto.address !== undefined ? dto.address?.trim() || null : company.address,
        logoUrl: uploadedLogo?.url ?? (dto.logoUrl !== undefined ? dto.logoUrl?.trim() || null : company.logoUrl)
      }
    });

    return mapCompany(updatedCompany);
  }

  async deleteCompany(_adminEmployeeId: string): Promise<void> {
    const company = await this.prisma.company.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: "desc" }
    });

    if (!company) {
      throw new HttpError(404, "Company not found");
    }

    await this.prisma.company.delete({
      where: { id: company.id }
    });
  }

  private async ensureCompanyUnique(companyName: string, email: string, currentId?: number): Promise<void> {
    const existingByName = await this.prisma.company.findFirst({
      where: {
        companyName,
        ...(currentId ? { id: { not: BigInt(currentId) } } : {})
      },
      select: { id: true }
    });

    if (existingByName) {
      throw new HttpError(400, "Company name already exists");
    }

    const existingByEmail = await this.prisma.company.findFirst({
      where: {
        email,
        ...(currentId ? { id: { not: BigInt(currentId) } } : {})
      },
      select: { id: true }
    });

    if (existingByEmail) {
      throw new HttpError(400, "Email already exists");
    }
  }
}

function mapCompany(company: Company): Record<string, unknown> {
  return {
    id: Number(company.id),
    companyName: company.companyName,
    email: company.email,
    contactNo: company.contactNo,
    website: company.website,
    address: company.address,
    logoUrl: company.logoUrl,
    logoId: null,
    isActive: company.isActive,
    createdAt: company.createdAt,
    updatedAt: company.updatedAt
  };
}

function validateContactNo(contactNo: string): void {
  if (!/^[0-9]{10,15}$/.test(contactNo)) {
    throw new HttpError(400, "Invalid contact number");
  }
}
