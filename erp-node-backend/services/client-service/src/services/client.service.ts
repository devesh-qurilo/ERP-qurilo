import type { Client, ClientDocument, ClientNote, Company, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import type { MediaStorageService } from "./media-storage.service.js";

export interface CompanyPayload {
  companyName?: string;
  website?: string;
  officePhone?: string;
  taxName?: string;
  gstVatNo?: string;
  address?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  shippingAddress?: string;
}

export interface ClientPayload {
  name: string;
  email: string;
  mobile: string;
  country?: string;
  gender?: string;
  category?: string;
  subCategory?: string;
  language?: string;
  receiveEmail?: boolean;
  skype?: string;
  linkedIn?: string;
  twitter?: string;
  facebook?: string;
  company?: CompanyPayload | null;
}

export interface ImportResultPayload {
  rowNumber: number;
  status: "CREATED" | "SKIPPED" | "ERROR";
  reason: string | null;
  createdId: number | null;
}

type ClientWithCompany = Client & { company: Company | null };
type NoteRecord = ClientNote;
type DocumentRecord = ClientDocument;

export class ClientService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly mediaStorageService: MediaStorageService
  ) {}

  async createClient(
    payload: ClientPayload,
    addedBy: string,
    profilePicture?: { filename: string | null; contentType: string | null; data: Buffer } | null,
    companyLogo?: { filename: string | null; contentType: string | null; data: Buffer } | null
  ) {
    validateRequired(payload.name, "Name is required");
    validateRequired(payload.email, "Email is required");
    validateRequired(payload.mobile, "Mobile is required");

    const email = payload.email.trim().toLowerCase();
    const mobile = payload.mobile.trim();

    if (await this.prisma.client.findUnique({ where: { email } })) {
      throw new HttpError(409, "Email already exists");
    }
    if (await this.prisma.client.findUnique({ where: { mobile } })) {
      throw new HttpError(409, "Mobile already exists");
    }

    const generatedClientId = await this.generateClientId();
    const profileUpload = await this.mediaStorageService.saveUploadedFile(profilePicture, "clients/profile");
    const companyLogoUpload = await this.mediaStorageService.saveUploadedFile(companyLogo, "clients/company");

    const client = await this.prisma.client.create({
      data: {
        clientId: generatedClientId,
        name: payload.name.trim(),
        email,
        mobile,
        country: payload.country ?? null,
        gender: payload.gender ?? null,
        category: payload.category ?? null,
        subCategory: payload.subCategory ?? null,
        profilePictureUrl: profileUpload?.url ?? null,
        language: payload.language ?? null,
        receiveEmail: payload.receiveEmail ?? true,
        status: "ACTIVE",
        skype: payload.skype ?? null,
        linkedIn: payload.linkedIn ?? null,
        twitter: payload.twitter ?? null,
        facebook: payload.facebook ?? null,
        companyLogoUrl: companyLogoUpload?.url ?? null,
        addedBy,
        company: payload.company
          ? {
              create: {
                companyName: payload.company.companyName ?? null,
                website: payload.company.website ?? null,
                officePhone: payload.company.officePhone ?? null,
                taxName: payload.company.taxName ?? null,
                gstVatNo: payload.company.gstVatNo ?? null,
                address: payload.company.address ?? null,
                city: payload.company.city ?? null,
                state: payload.company.state ?? null,
                postalCode: payload.company.postalCode ?? null,
                shippingAddress: payload.company.shippingAddress ?? null,
                companyLogoUrl: companyLogoUpload?.url ?? null
              }
            }
          : undefined
      },
      include: { company: true }
    });

    return this.mapClient(client);
  }

  async listClients(search?: string | null, status?: string | null) {
    const clients = await this.prisma.client.findMany({
      where: {
        AND: [
          search
            ? {
                OR: [
                  { name: { contains: search, mode: "insensitive" } },
                  { email: { contains: search, mode: "insensitive" } },
                  { mobile: { contains: search, mode: "insensitive" } },
                  { clientId: { contains: search, mode: "insensitive" } }
                ]
              }
            : {},
          status ? { status: { equals: status, mode: "insensitive" } } : {}
        ]
      },
      include: { company: true },
      orderBy: { id: "desc" }
    });

    return clients.map((client) => this.mapClient(client));
  }

  async getClient(id: number) {
    const client = await this.prisma.client.findUnique({ where: { id }, include: { company: true } });
    if (!client) {
      throw new HttpError(404, "Client not found");
    }
    return this.mapClient(client);
  }

  async getClientByClientId(clientId: string) {
    const client = await this.prisma.client.findUnique({
      where: { clientId: clientId.trim() },
      include: { company: true }
    });
    if (!client) {
      throw new HttpError(404, "Not found");
    }
    return this.mapClient(client);
  }

  async updateClient(
    id: number,
    payload: ClientPayload,
    updatedBy: string,
    profilePicture?: { filename: string | null; contentType: string | null; data: Buffer } | null,
    companyLogo?: { filename: string | null; contentType: string | null; data: Buffer } | null
  ) {
    const existing = await this.prisma.client.findUnique({ where: { id }, include: { company: true } });
    if (!existing) {
      throw new HttpError(404, "Client not found");
    }

    if (payload.email && payload.email.trim().toLowerCase() !== existing.email.toLowerCase()) {
      if (await this.prisma.client.findUnique({ where: { email: payload.email.trim().toLowerCase() } })) {
        throw new HttpError(409, "Email already exists");
      }
    }
    if (payload.mobile && payload.mobile.trim() !== existing.mobile) {
      if (await this.prisma.client.findUnique({ where: { mobile: payload.mobile.trim() } })) {
        throw new HttpError(409, "Mobile already exists");
      }
    }

    const profileUpload = await this.mediaStorageService.saveUploadedFile(profilePicture, "clients/profile");
    const companyLogoUpload = await this.mediaStorageService.saveUploadedFile(companyLogo, "clients/company");

    const client = await this.prisma.client.update({
      where: { id },
      data: {
        name: payload.name?.trim() || existing.name,
        email: payload.email ? payload.email.trim().toLowerCase() : existing.email,
        mobile: payload.mobile?.trim() || existing.mobile,
        country: payload.country === undefined ? existing.country : payload.country ?? null,
        gender: payload.gender === undefined ? existing.gender : payload.gender ?? null,
        category: payload.category === undefined ? existing.category : payload.category ?? null,
        subCategory: payload.subCategory === undefined ? existing.subCategory : payload.subCategory ?? null,
        profilePictureUrl: profileUpload?.url ?? existing.profilePictureUrl,
        language: payload.language === undefined ? existing.language : payload.language ?? null,
        receiveEmail: payload.receiveEmail ?? existing.receiveEmail,
        skype: payload.skype === undefined ? existing.skype : payload.skype ?? null,
        linkedIn: payload.linkedIn === undefined ? existing.linkedIn : payload.linkedIn ?? null,
        twitter: payload.twitter === undefined ? existing.twitter : payload.twitter ?? null,
        facebook: payload.facebook === undefined ? existing.facebook : payload.facebook ?? null,
        companyLogoUrl: companyLogoUpload?.url ?? existing.companyLogoUrl,
        addedBy: updatedBy,
        company: payload.company
          ? existing.company
            ? {
                update: {
                  companyName: payload.company.companyName ?? existing.company.companyName,
                  website: payload.company.website ?? existing.company.website,
                  officePhone: payload.company.officePhone ?? existing.company.officePhone,
                  taxName: payload.company.taxName ?? existing.company.taxName,
                  gstVatNo: payload.company.gstVatNo ?? existing.company.gstVatNo,
                  address: payload.company.address ?? existing.company.address,
                  city: payload.company.city ?? existing.company.city,
                  state: payload.company.state ?? existing.company.state,
                  postalCode: payload.company.postalCode ?? existing.company.postalCode,
                  shippingAddress: payload.company.shippingAddress ?? existing.company.shippingAddress,
                  companyLogoUrl: companyLogoUpload?.url ?? existing.company.companyLogoUrl
                }
              }
            : {
                create: {
                  companyName: payload.company.companyName ?? null,
                  website: payload.company.website ?? null,
                  officePhone: payload.company.officePhone ?? null,
                  taxName: payload.company.taxName ?? null,
                  gstVatNo: payload.company.gstVatNo ?? null,
                  address: payload.company.address ?? null,
                  city: payload.company.city ?? null,
                  state: payload.company.state ?? null,
                  postalCode: payload.company.postalCode ?? null,
                  shippingAddress: payload.company.shippingAddress ?? null,
                  companyLogoUrl: companyLogoUpload?.url ?? null
                }
              }
          : undefined
      },
      include: { company: true }
    });

    return this.mapClient(client);
  }

  async deleteClient(id: number) {
    const existing = await this.prisma.client.findUnique({ where: { id } });
    if (!existing) {
      throw new HttpError(404, "Client not found");
    }
    await this.prisma.client.delete({ where: { id } });
  }

  async clientExistsByEmail(email: string): Promise<boolean> {
    return Boolean(await this.prisma.client.findUnique({ where: { email: email.trim().toLowerCase() } }));
  }

  async createCategory(name: string) {
    validateRequired(name, "Category name is required");
    return this.prisma.clientCategory.create({ data: { name: name.trim() } });
  }

  async createSubCategory(name: string) {
    validateRequired(name, "Subcategory name is required");
    return this.prisma.clientSubCategory.create({ data: { name: name.trim() } });
  }

  async listCategories() {
    return this.prisma.clientCategory.findMany({ orderBy: { id: "asc" } });
  }

  async listSubCategories() {
    return this.prisma.clientSubCategory.findMany({ orderBy: { id: "asc" } });
  }

  async deleteCategory(id: number) {
    await this.prisma.clientCategory.delete({ where: { id } });
    return { message: "SUCCESS" };
  }

  async deleteSubCategory(id: number) {
    await this.prisma.clientSubCategory.delete({ where: { id } });
    return { message: "SUCCESS" };
  }

  async addNote(clientId: number, dto: { title: string; detail: string; type?: string | null }, createdBy: string) {
    await this.ensureClient(clientId);
    return this.mapNote(
      await this.prisma.clientNote.create({
        data: {
          clientId,
          title: dto.title.trim(),
          detail: dto.detail.trim(),
          type: dto.type ?? "PUBLIC",
          createdBy
        }
      })
    );
  }

  async listNotes(clientId: number) {
    await this.ensureClient(clientId);
    const notes = await this.prisma.clientNote.findMany({ where: { clientId }, orderBy: { id: "desc" } });
    return notes.map((note) => this.mapNote(note));
  }

  async updateNote(noteId: number, dto: { title: string; detail: string; type?: string | null }) {
    const note = await this.prisma.clientNote.findUnique({ where: { id: noteId } });
    if (!note) {
      throw new HttpError(404, "Note not found");
    }
    return this.mapNote(
      await this.prisma.clientNote.update({
        where: { id: noteId },
        data: {
          title: dto.title.trim(),
          detail: dto.detail.trim(),
          type: dto.type ?? note.type
        }
      })
    );
  }

  async deleteNote(noteId: number) {
    await this.prisma.clientNote.delete({ where: { id: noteId } });
  }

  async uploadDocument(
    clientId: number,
    uploadedBy: string,
    file?: { filename: string | null; contentType: string | null; data: Buffer } | null
  ) {
    await this.ensureClient(clientId);
    const uploaded = await this.mediaStorageService.saveUploadedFile(file, `client-documents/${clientId}`);
    const filename = file?.filename?.trim() ?? null;

    if (!uploaded || !filename) {
      throw new HttpError(400, "file is required");
    }

    return this.mapDocument(
      await this.prisma.clientDocument.create({
        data: {
          clientId,
          filename,
          url: uploaded.url,
          objectKey: uploaded.objectKey,
          mimeType: file?.contentType ?? null,
          size: file?.data?.length ? BigInt(file.data.length) : null,
          uploadedBy
        }
      })
    );
  }

  async listDocuments(clientId: number) {
    await this.ensureClient(clientId);
    const documents = await this.prisma.clientDocument.findMany({ where: { clientId }, orderBy: { uploadedAt: "desc" } });
    return documents.map((document) => this.mapDocument(document));
  }

  async getDocument(clientId: number, docId: number) {
    await this.ensureClient(clientId);
    const document = await this.prisma.clientDocument.findUnique({ where: { id: docId } });
    if (!document || document.clientId !== clientId) {
      throw new HttpError(404, "Document not found");
    }
    return this.mapDocument(document);
  }

  async deleteDocument(clientId: number, docId: number) {
    await this.ensureClient(clientId);
    const document = await this.prisma.clientDocument.findUnique({ where: { id: docId } });
    if (!document || document.clientId !== clientId) {
      throw new HttpError(404, "Document not found");
    }
    try {
      await this.mediaStorageService.deleteUploadedFile(document.objectKey);
    } catch {
      // Keep DB cleanup resilient even if remote delete fails.
    }
    await this.prisma.clientDocument.delete({ where: { id: docId } });
  }

  async importClientsFromCsv(file: { filename: string | null; contentType: string | null; data: Buffer } | null, actor: string) {
    if (!file?.data?.length) {
      return [{ rowNumber: 0, status: "ERROR", reason: "Empty or missing file", createdId: null }] satisfies ImportResultPayload[];
    }

    const rows = parseCsvRows(file.data.toString("utf8"));
    if (!rows.length) {
      return [{ rowNumber: 0, status: "ERROR", reason: "Failed to parse file: no rows found", createdId: null }] satisfies ImportResultPayload[];
    }

    const [headerRow, ...dataRows] = rows;
    const headerMap = buildHeaderMap(headerRow);
    const existing = await this.prisma.client.findMany({ include: { company: false } });
    const results: ImportResultPayload[] = [];

    for (const [index, row] of dataRows.entries()) {
      const rowNumber = index + 2;
      try {
        const mapped = mapClientImportRow(row, headerMap);
        const name = safeTrim(mapped.name);
        const email = safeTrim(mapped.email)?.toLowerCase() ?? null;
        const mobile = safeTrim(mapped.mobile);
        const mobileDigits = normalizeDigits(mobile);

        if (!name && !email && !mobile) {
          results.push({ rowNumber, status: "SKIPPED", reason: "Empty row (no name/email/mobile)", createdId: null });
          continue;
        }

        if (email && existing.some((client) => client.email.toLowerCase() === email)) {
          results.push({ rowNumber, status: "SKIPPED", reason: "Duplicate email", createdId: null });
          continue;
        }

        if (mobileDigits && existing.some((client) => normalizeDigits(client.mobile) === mobileDigits)) {
          results.push({ rowNumber, status: "SKIPPED", reason: "Duplicate mobile", createdId: null });
          continue;
        }

        const created = await this.prisma.client.create({
          data: {
            clientId: await this.generateClientId(),
            name: name ?? "",
            email: email ?? `missing-${Date.now()}-${rowNumber}@example.com`,
            mobile: mobile ?? `missing-${Date.now()}-${rowNumber}`,
            country: safeTrim(mapped.country),
            status: "ACTIVE",
            addedBy: actor,
            company: mapped.companyName || mapped.website || mapped.officePhone || mapped.gstVatNo
              ? {
                  create: {
                    companyName: safeTrim(mapped.companyName),
                    website: safeTrim(mapped.website),
                    officePhone: safeTrim(mapped.officePhone),
                    gstVatNo: safeTrim(mapped.gstVatNo),
                    city: safeTrim(mapped.city),
                    state: safeTrim(mapped.state),
                    postalCode: safeTrim(mapped.postalCode)
                  }
                }
              : undefined
          }
        });

        existing.push({ ...created, company: null } as Client & { company: null });
        results.push({ rowNumber, status: "CREATED", reason: null, createdId: created.id });
      } catch (error) {
        results.push({
          rowNumber,
          status: "ERROR",
          reason: `Unhandled: ${error instanceof Error ? error.message : "Unknown error"}`,
          createdId: null
        });
      }
    }

    return results;
  }

  private async ensureClient(id: number) {
    const client = await this.prisma.client.findUnique({ where: { id } });
    if (!client) {
      throw new HttpError(404, "Client not found");
    }
    return client;
  }

  private async generateClientId(): Promise<string> {
    const last = await this.prisma.client.findFirst({ orderBy: { id: "desc" } });
    let nextNum = 1;
    if (last?.clientId) {
      const numeric = last.clientId.replaceAll(/\D/g, "");
      const parsed = Number(numeric);
      nextNum = Number.isFinite(parsed) && parsed > 0 ? parsed + 1 : 1;
    }

    let generated: string;
    do {
      generated = `CLI${String(nextNum).padStart(3, "0")}`;
      nextNum += 1;
    } while (await this.prisma.client.findUnique({ where: { clientId: generated } }));

    return generated;
  }

  private mapClient(client: ClientWithCompany) {
    return {
      id: client.id,
      clientId: client.clientId,
      name: client.name,
      email: client.email,
      mobile: client.mobile,
      country: client.country,
      gender: client.gender,
      category: client.category,
      subCategory: client.subCategory,
      profilePictureUrl: client.profilePictureUrl,
      language: client.language,
      receiveEmail: client.receiveEmail,
      status: client.status,
      skype: client.skype,
      linkedIn: client.linkedIn,
      twitter: client.twitter,
      facebook: client.facebook,
      company: client.company
        ? {
            companyName: client.company.companyName,
            website: client.company.website,
            officePhone: client.company.officePhone,
            taxName: client.company.taxName,
            gstVatNo: client.company.gstVatNo,
            address: client.company.address,
            city: client.company.city,
            state: client.company.state,
            postalCode: client.company.postalCode,
            shippingAddress: client.company.shippingAddress,
            companyLogoUrl: client.company.companyLogoUrl
          }
        : null,
      companyLogoUrl: client.companyLogoUrl,
      addedBy: client.addedBy,
      createdAt: client.createdAt.toISOString()
    };
  }

  private mapNote(note: NoteRecord) {
    return {
      id: note.id,
      clientId: note.clientId,
      title: note.title,
      detail: note.detail,
      type: note.type,
      createdBy: note.createdBy,
      createdAt: note.createdAt.toISOString()
    };
  }

  private mapDocument(document: DocumentRecord) {
    return {
      id: document.id,
      filename: document.filename,
      url: document.url,
      mimeType: document.mimeType,
      size: document.size ? Number(document.size) : null,
      uploadedAt: document.uploadedAt.toISOString(),
      uploadedBy: document.uploadedBy
    };
  }
}

function validateRequired(value: string | undefined | null, message: string): void {
  if (!value?.trim()) {
    throw new HttpError(400, message);
  }
}

function parseCsvRows(input: string): string[][] {
  const rows: string[][] = [];
  let currentField = "";
  let currentRow: string[] = [];
  let inQuotes = false;

  for (let index = 0; index < input.length; index += 1) {
    const char = input[index];
    const nextChar = input[index + 1];

    if (char === "\"") {
      if (inQuotes && nextChar === "\"") {
        currentField += "\"";
        index += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }

    if (char === "," && !inQuotes) {
      currentRow.push(currentField);
      currentField = "";
      continue;
    }

    if ((char === "\n" || char === "\r") && !inQuotes) {
      if (char === "\r" && nextChar === "\n") {
        index += 1;
      }
      currentRow.push(currentField);
      currentField = "";
      if (currentRow.some((field) => field.length > 0)) {
        rows.push(currentRow);
      }
      currentRow = [];
      continue;
    }

    currentField += char;
  }

  currentRow.push(currentField);
  if (currentRow.some((field) => field.length > 0)) {
    rows.push(currentRow);
  }

  return rows;
}

function buildHeaderMap(headerRow: string[]): Map<string, number> {
  const headerMap = new Map<string, number>();
  headerRow.forEach((header, index) => {
    const normalized = normalizeHeaderKey(header);
    if (normalized && !headerMap.has(normalized)) {
      headerMap.set(normalized, index);
    }
  });
  return headerMap;
}

function mapClientImportRow(row: string[], headerMap: Map<string, number>) {
  return {
    name: getCsvValue(row, headerMap, ["name", "full name", "client name", "contact name"]),
    email: getCsvValue(row, headerMap, ["email", "e-mail", "email address"]),
    mobile: getCsvValue(row, headerMap, ["mobile", "mobile number", "phone", "phone number", "contact"]),
    country: getCsvValue(row, headerMap, ["country", "nation"]),
    companyName: getCsvValue(row, headerMap, ["company", "company name", "organization", "org"]),
    website: getCsvValue(row, headerMap, ["website", "url", "company website"]),
    officePhone: getCsvValue(row, headerMap, ["officephone", "office phone", "landline"]),
    city: getCsvValue(row, headerMap, ["city"]),
    state: getCsvValue(row, headerMap, ["state", "province", "region"]),
    postalCode: getCsvValue(row, headerMap, ["postalcode", "postal code", "zip", "zip code"]),
    gstVatNo: getCsvValue(row, headerMap, ["gst", "vat", "gst_vat_no", "taxno"])
  };
}

function getCsvValue(row: string[], headerMap: Map<string, number>, keys: string[]): string | null {
  for (const key of keys) {
    const columnIndex = headerMap.get(normalizeHeaderKey(key));
    if (columnIndex !== undefined) {
      return safeTrim(row[columnIndex] ?? "");
    }
  }
  return null;
}

function normalizeHeaderKey(header: string): string {
  return header.trim().toLowerCase().replaceAll(/\s+/g, "");
}

function safeTrim(value: string | null | undefined): string | null {
  return value == null ? null : value.trim();
}

function normalizeDigits(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }
  const digits = value.replaceAll(/\D+/g, "");
  return digits || null;
}
