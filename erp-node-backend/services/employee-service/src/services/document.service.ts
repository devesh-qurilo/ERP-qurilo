import type { Employee, EmployeeDocument, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import type { EmployeeDocumentUploadDto } from "../modules/document/dto.js";

type EmployeeDocumentWithEmployee = EmployeeDocument & {
  employee: Employee;
};

export class DocumentService {
  constructor(private readonly prisma: PrismaClient) {}

  async uploadDocument(
    employeeId: string,
    dto: EmployeeDocumentUploadDto,
    uploadedBy: string
  ): Promise<Record<string, unknown>> {
    const employee = await this.ensureEmployee(employeeId);
    const filename = dto.filename?.trim();
    const url = dto.url?.trim();

    if (!filename || !url) {
      throw new HttpError(400, "filename and url are required");
    }

    const document = await this.prisma.employeeDocument.create({
      data: {
        employeeId: employee.employeeId,
        bucket: dto.bucket?.trim() || "employee-documents",
        path: dto.path?.trim() || buildDocumentPath(employee.employeeId, filename),
        filename,
        mime: dto.mime?.trim() || null,
        size: dto.size !== undefined && dto.size !== null ? BigInt(dto.size) : null,
        url,
        uploadedBy: dto.uploadedBy?.trim() || uploadedBy,
        entityType: "DOCUMENT"
      },
      include: { employee: true }
    });

    return mapDocument(document);
  }

  async getAllDocuments(employeeId: string): Promise<Record<string, unknown>[]> {
    await this.ensureEmployee(employeeId);

    const documents = await this.prisma.employeeDocument.findMany({
      where: { employeeId: normalizeEmployeeId(employeeId) },
      include: { employee: true },
      orderBy: { uploadedAt: "desc" }
    });

    return documents.map(mapDocument);
  }

  async getDocument(employeeId: string, docId: number): Promise<Record<string, unknown>> {
    const document = await this.findDocumentOrThrow(employeeId, docId);
    return mapDocument(document);
  }

  async deleteDocument(employeeId: string, docId: number): Promise<void> {
    await this.findDocumentOrThrow(employeeId, docId);
    await this.prisma.employeeDocument.delete({
      where: { id: BigInt(docId) }
    });
  }

  async getDownloadUrl(employeeId: string, docId: number): Promise<string> {
    const document = await this.findDocumentOrThrow(employeeId, docId);
    return document.url;
  }

  private async ensureEmployee(employeeId: string): Promise<Employee> {
    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: normalizeEmployeeId(employeeId) }
    });

    if (!employee) {
      throw new HttpError(404, `Employee not found: ${employeeId}`);
    }

    return employee;
  }

  private async findDocumentOrThrow(employeeId: string, docId: number): Promise<EmployeeDocumentWithEmployee> {
    await this.ensureEmployee(employeeId);

    const document = await this.prisma.employeeDocument.findFirst({
      where: {
        id: BigInt(docId),
        employeeId: normalizeEmployeeId(employeeId)
      },
      include: { employee: true }
    });

    if (!document) {
      throw new HttpError(404, `Document not found with id ${docId}`);
    }

    return document;
  }
}

function mapDocument(document: EmployeeDocumentWithEmployee): Record<string, unknown> {
  return {
    id: Number(document.id),
    bucket: document.bucket,
    path: document.path,
    filename: document.filename,
    mime: document.mime,
    size: document.size ? Number(document.size) : null,
    url: document.url,
    uploadedBy: document.uploadedBy,
    entityType: document.entityType,
    uploadedAt: document.uploadedAt,
    employeeId: document.employee.employeeId
  };
}

function normalizeEmployeeId(employeeId: string): string {
  return employeeId.trim().toUpperCase();
}

function buildDocumentPath(employeeId: string, filename: string): string {
  return `employee-documents/${employeeId}/${filename}`;
}
