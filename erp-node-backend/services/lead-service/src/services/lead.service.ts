import type { PrismaClient, Prisma } from "@prisma/client";

import type { AuthContext } from "@erp/shared-auth";

import { HttpError } from "../common/errors.js";
import type { EmployeeClient } from "../lib/employee-client.js";

export interface LeadPayload {
  name: string;
  email?: string;
  clientCategory?: string;
  leadSource?: string;
  leadOwner?: string;
  addedBy?: string;
  createDeal?: boolean;
  autoConvertToClient?: boolean;
  companyName?: string;
  officialWebsite?: string;
  mobileNumber?: string;
  officePhone?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  companyAddress?: string;
}

export interface DealPayload {
  title: string;
  value?: number;
  currency?: string;
  dealStage?: string;
  dealAgent?: string;
  dealWatchers?: string[];
  leadId?: number;
  expectedCloseDate?: string;
  pipeline?: string;
  dealCategory?: string;
  dealContact?: string;
}

export interface StagePayload {
  name: string;
  color?: string;
  position?: number;
}

export interface CategoryPayload {
  name: string;
  color?: string;
}

export interface NotePayload {
  note: string;
}

export interface TagPayload {
  tagName: string;
}

export interface PriorityPayload {
  status: string;
  color?: string;
}

export class LeadService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly employeeClient: EmployeeClient
  ) {}

  async createLead(payload: LeadPayload, auth: AuthContext, authorizationHeader?: string) {
    if (!payload.name?.trim()) {
      throw new HttpError(400, "Lead name is required");
    }

    const leadOwner = payload.leadOwner ?? auth.userId;
    const addedBy = payload.addedBy ?? auth.userId;

    if (auth.role !== "ROLE_ADMIN") {
      if (payload.leadOwner && payload.leadOwner !== auth.userId) {
        throw new HttpError(403, "Only admins can assign other lead owners");
      }
    }

    await this.employeeClient.ensureEmployeeExists(leadOwner, authorizationHeader);
    await this.employeeClient.ensureEmployeeExists(addedBy, authorizationHeader);

    if (payload.email) {
      const existingByEmail = await this.prisma.lead.findUnique({ where: { email: payload.email } });
      if (existingByEmail) {
        throw new HttpError(409, "Lead with this email already exists");
      }
    }

    if (payload.mobileNumber) {
      const existingByMobile = await this.prisma.lead.findUnique({ where: { mobileNumber: payload.mobileNumber } });
      if (existingByMobile) {
        throw new HttpError(409, "Lead with this mobile number already exists");
      }
    }

    const lead = await this.prisma.lead.create({
      data: {
        name: payload.name.trim(),
        email: payload.email?.trim() || null,
        clientCategory: payload.clientCategory ?? null,
        leadSource: payload.leadSource ?? null,
        leadOwner,
        addedBy,
        createDeal: Boolean(payload.createDeal),
        autoConvertToClient: Boolean(payload.autoConvertToClient),
        companyName: payload.companyName ?? null,
        officialWebsite: payload.officialWebsite ?? null,
        mobileNumber: payload.mobileNumber ?? null,
        officePhone: payload.officePhone ?? null,
        city: payload.city ?? null,
        state: payload.state ?? null,
        postalCode: payload.postalCode ?? null,
        country: payload.country ?? null,
        companyAddress: payload.companyAddress ?? null
      }
    });

    return this.enrichLead(lead);
  }

  async getLeadById(id: number, auth: AuthContext) {
    const lead = await this.prisma.lead.findUnique({ where: { id } });

    if (!lead) {
      throw new HttpError(404, "Lead not found");
    }

    this.ensureLeadAccess(lead, auth);
    return this.enrichLead(lead);
  }

  async getAllLeads(auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can access all leads");
    }

    const leads = await this.prisma.lead.findMany({ orderBy: { id: "desc" } });
    return Promise.all(leads.map((lead) => this.enrichLead(lead)));
  }

  async getMyLeads(auth: AuthContext) {
    const leads = await this.prisma.lead.findMany({
      where: {
        OR: [
          { leadOwner: auth.userId },
          { addedBy: auth.userId }
        ]
      },
      orderBy: { id: "desc" }
    });

    return Promise.all(leads.map((lead) => this.enrichLead(lead)));
  }

  async updateLead(id: number, payload: LeadPayload, auth: AuthContext, authorizationHeader?: string) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can update leads");
    }

    const existing = await this.prisma.lead.findUnique({ where: { id } });

    if (!existing) {
      throw new HttpError(404, "Lead not found");
    }

    if (payload.email && payload.email !== existing.email) {
      const duplicate = await this.prisma.lead.findUnique({ where: { email: payload.email } });
      if (duplicate) {
        throw new HttpError(409, "Lead with this email already exists");
      }
    }

    if (payload.mobileNumber && payload.mobileNumber !== existing.mobileNumber) {
      const duplicate = await this.prisma.lead.findUnique({ where: { mobileNumber: payload.mobileNumber } });
      if (duplicate) {
        throw new HttpError(409, "Lead with this mobile number already exists");
      }
    }

    if (payload.leadOwner) {
      await this.employeeClient.ensureEmployeeExists(payload.leadOwner, authorizationHeader);
    }

    if (payload.addedBy) {
      await this.employeeClient.ensureEmployeeExists(payload.addedBy, authorizationHeader);
    }

    const lead = await this.prisma.lead.update({
      where: { id },
      data: {
        name: payload.name?.trim() || existing.name,
        email: payload.email === undefined ? existing.email : payload.email || null,
        clientCategory: payload.clientCategory === undefined ? existing.clientCategory : payload.clientCategory ?? null,
        leadSource: payload.leadSource === undefined ? existing.leadSource : payload.leadSource ?? null,
        leadOwner: payload.leadOwner ?? existing.leadOwner,
        addedBy: payload.addedBy ?? existing.addedBy,
        createDeal: payload.createDeal ?? existing.createDeal,
        autoConvertToClient: payload.autoConvertToClient ?? existing.autoConvertToClient,
        companyName: payload.companyName === undefined ? existing.companyName : payload.companyName ?? null,
        officialWebsite: payload.officialWebsite === undefined ? existing.officialWebsite : payload.officialWebsite ?? null,
        mobileNumber: payload.mobileNumber === undefined ? existing.mobileNumber : payload.mobileNumber ?? null,
        officePhone: payload.officePhone === undefined ? existing.officePhone : payload.officePhone ?? null,
        city: payload.city === undefined ? existing.city : payload.city ?? null,
        state: payload.state === undefined ? existing.state : payload.state ?? null,
        postalCode: payload.postalCode === undefined ? existing.postalCode : payload.postalCode ?? null,
        country: payload.country === undefined ? existing.country : payload.country ?? null,
        companyAddress: payload.companyAddress === undefined ? existing.companyAddress : payload.companyAddress ?? null
      }
    });

    return this.enrichLead(lead);
  }

  async deleteLead(id: number, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can delete leads");
    }

    const existing = await this.prisma.lead.findUnique({ where: { id } });

    if (!existing) {
      throw new HttpError(404, "Lead not found");
    }

    await this.prisma.lead.delete({ where: { id } });
  }

  async getLeadDealStats(id: number, auth: AuthContext) {
    const lead = await this.prisma.lead.findUnique({ where: { id } });

    if (!lead) {
      throw new HttpError(404, "Lead not found");
    }

    this.ensureLeadAccess(lead, auth);

    const deals = await this.prisma.deal.findMany({ where: { leadId: id } });
    const totalValue = deals.reduce((sum, deal) => sum + (deal.value ?? 0), 0);

    return {
      leadId: id,
      dealCount: deals.length,
      totalDealValue: totalValue,
      openDeals: deals.filter((deal) => deal.dealStage !== "WIN" && deal.dealStage !== "LOSE").length,
      wonDeals: deals.filter((deal) => deal.dealStage === "WIN").length,
      lostDeals: deals.filter((deal) => deal.dealStage === "LOSE").length
    };
  }

  async createDeal(payload: DealPayload, auth: AuthContext, authorizationHeader?: string) {
    if (!payload.title?.trim()) {
      throw new HttpError(400, "Deal title is required");
    }

    if (payload.dealAgent) {
      await this.employeeClient.ensureEmployeeExists(payload.dealAgent, authorizationHeader);
    }

    for (const watcher of payload.dealWatchers ?? []) {
      await this.employeeClient.ensureEmployeeExists(watcher, authorizationHeader);
    }

    if (payload.leadId) {
      const lead = await this.prisma.lead.findUnique({ where: { id: payload.leadId } });
      if (!lead) {
        throw new HttpError(404, "Lead not found");
      }
      this.ensureLeadAccess(lead, auth);
    } else if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Non-admin users can only create deals for their accessible leads");
    }

    const deal = await this.prisma.deal.create({
      data: {
        title: payload.title.trim(),
        value: payload.value ?? null,
        currency: payload.currency ?? null,
        dealStage: payload.dealStage ?? null,
        dealAgent: payload.dealAgent ?? null,
        dealContact: payload.dealContact ?? null,
        expectedCloseDate: payload.expectedCloseDate ? new Date(payload.expectedCloseDate) : null,
        pipeline: payload.pipeline ?? null,
        dealCategory: payload.dealCategory ?? null,
        dealWatchers: payload.dealWatchers ? payload.dealWatchers : [],
        leadId: payload.leadId ?? null
      }
    });

    return this.enrichDeal(deal);
  }

  async getDealById(id: number, auth: AuthContext) {
    const deal = await this.prisma.deal.findUnique({ where: { id } });

    if (!deal) {
      throw new HttpError(404, "Deal not found");
    }

    await this.ensureDealAccess(deal, auth);
    return this.enrichDeal(deal);
  }

  async getDealsByLeadId(leadId: number, auth: AuthContext) {
    const lead = await this.prisma.lead.findUnique({ where: { id: leadId } });

    if (!lead) {
      throw new HttpError(404, "Lead not found");
    }

    this.ensureLeadAccess(lead, auth);

    const deals = await this.prisma.deal.findMany({
      where: { leadId },
      orderBy: { id: "desc" }
    });

    return Promise.all(deals.map((deal) => this.enrichDeal(deal)));
  }

  async getAllDeals(auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can access all deals");
    }

    const deals = await this.prisma.deal.findMany({ orderBy: { id: "desc" } });
    return Promise.all(deals.map((deal) => this.enrichDeal(deal)));
  }

  async getDealTags(dealId: number, auth: AuthContext) {
    await this.getDealById(dealId, auth);
    return this.prisma.dealTag.findMany({
      where: { dealId },
      orderBy: { id: "desc" }
    });
  }

  async addDealTag(dealId: number, payload: TagPayload, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can add tags to deals");
    }

    const deal = await this.prisma.deal.findUnique({ where: { id: dealId } });
    if (!deal) {
      throw new HttpError(404, "Deal not found");
    }

    if (!payload.tagName?.trim()) {
      throw new HttpError(400, "tagName is required");
    }

    await this.prisma.dealTag.create({
      data: {
        dealId,
        tagName: payload.tagName.trim()
      }
    });
  }

  async deleteDealTag(dealId: number, tagId: number, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can delete deal tags");
    }

    const tag = await this.prisma.dealTag.findUnique({ where: { id: tagId } });
    if (!tag || tag.dealId !== dealId) {
      throw new HttpError(404, "Tag not found for this deal");
    }

    await this.prisma.dealTag.delete({ where: { id: tagId } });
  }

  async updateDeal(id: number, payload: DealPayload, auth: AuthContext, authorizationHeader?: string) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can update deals");
    }

    const existing = await this.prisma.deal.findUnique({ where: { id } });

    if (!existing) {
      throw new HttpError(404, "Deal not found");
    }

    if (payload.dealAgent) {
      await this.employeeClient.ensureEmployeeExists(payload.dealAgent, authorizationHeader);
    }

    for (const watcher of payload.dealWatchers ?? []) {
      await this.employeeClient.ensureEmployeeExists(watcher, authorizationHeader);
    }

    if (payload.leadId) {
      const lead = await this.prisma.lead.findUnique({ where: { id: payload.leadId } });
      if (!lead) {
        throw new HttpError(404, "Lead not found");
      }
    }

    const deal = await this.prisma.deal.update({
      where: { id },
      data: {
        title: payload.title?.trim() || existing.title,
        value: payload.value === undefined ? existing.value : payload.value ?? null,
        currency: payload.currency === undefined ? existing.currency : payload.currency ?? null,
        dealStage: payload.dealStage === undefined ? existing.dealStage : payload.dealStage ?? null,
        dealAgent: payload.dealAgent === undefined ? existing.dealAgent : payload.dealAgent ?? null,
        dealContact: payload.dealContact === undefined ? existing.dealContact : payload.dealContact ?? null,
        expectedCloseDate: payload.expectedCloseDate === undefined
          ? existing.expectedCloseDate
          : payload.expectedCloseDate
            ? new Date(payload.expectedCloseDate)
            : null,
        pipeline: payload.pipeline === undefined ? existing.pipeline : payload.pipeline ?? null,
        dealCategory: payload.dealCategory === undefined ? existing.dealCategory : payload.dealCategory ?? null,
        dealWatchers: payload.dealWatchers === undefined
          ? (existing.dealWatchers as Prisma.InputJsonValue | Prisma.NullableJsonNullValueInput | undefined)
          : payload.dealWatchers,
        leadId: payload.leadId === undefined ? existing.leadId : payload.leadId ?? null
      }
    });

    return this.enrichDeal(deal);
  }

  async updateDealStage(id: number, stage: string, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can update deal stage");
    }

    const existing = await this.prisma.deal.findUnique({ where: { id } });

    if (!existing) {
      throw new HttpError(404, "Deal not found");
    }

    const deal = await this.prisma.deal.update({
      where: { id },
      data: { dealStage: stage }
    });

    return this.enrichDeal(deal);
  }

  async deleteDeal(id: number, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can delete deals");
    }

    const existing = await this.prisma.deal.findUnique({ where: { id } });
    if (!existing) {
      throw new HttpError(404, "Deal not found");
    }

    await this.prisma.deal.delete({ where: { id } });
  }

  async getGlobalDealStats(auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can access deal stats");
    }

    const deals = await this.prisma.deal.findMany();

    return {
      totalDeals: deals.length,
      totalValue: deals.reduce((sum, deal) => sum + (deal.value ?? 0), 0),
      wonDeals: deals.filter((deal) => deal.dealStage === "WIN").length,
      lostDeals: deals.filter((deal) => deal.dealStage === "LOSE").length,
      openDeals: deals.filter((deal) => deal.dealStage !== "WIN" && deal.dealStage !== "LOSE").length
    };
  }

  async createStage(payload: StagePayload, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can create stages");
    }

    return this.prisma.stage.create({
      data: {
        name: payload.name.trim(),
        color: payload.color ?? null,
        position: payload.position ?? null
      }
    });
  }

  async listStages(auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can view stages");
    }

    return this.prisma.stage.findMany({ orderBy: [{ position: "asc" }, { id: "asc" }] });
  }

  async getStage(id: number, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can view a stage");
    }

    const stage = await this.prisma.stage.findUnique({ where: { id } });

    if (!stage) {
      throw new HttpError(404, "Stage not found");
    }

    return stage;
  }

  async updateStage(id: number, payload: StagePayload, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can update stages");
    }

    await this.getStage(id, auth);

    return this.prisma.stage.update({
      where: { id },
      data: {
        name: payload.name.trim(),
        color: payload.color ?? null,
        position: payload.position ?? null
      }
    });
  }

  async deleteStage(id: number, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can delete stages");
    }

    await this.getStage(id, auth);
    await this.prisma.stage.delete({ where: { id } });
  }

  async createDealCategory(payload: CategoryPayload) {
    return this.prisma.dealCategory.create({
      data: {
        name: payload.name.trim(),
        color: payload.color ?? null
      }
    });
  }

  async listDealCategories() {
    return this.prisma.dealCategory.findMany({ orderBy: { id: "asc" } });
  }

  async deleteDealCategory(id: number) {
    await this.prisma.dealCategory.delete({ where: { id } });
    return { message: "Successful Delete" };
  }

  async createLeadSource(payload: CategoryPayload) {
    return this.prisma.leadSource.create({
      data: {
        name: payload.name.trim(),
        color: payload.color ?? null
      }
    });
  }

  async listLeadSources() {
    return this.prisma.leadSource.findMany({ orderBy: { id: "asc" } });
  }

  async deleteLeadSource(id: number) {
    await this.prisma.leadSource.delete({ where: { id } });
    return { message: "Successful Delete" };
  }

  async addLeadNote(leadId: number, payload: NotePayload, auth: AuthContext) {
    await this.getLeadById(leadId, auth);

    return this.prisma.leadNote.create({
      data: {
        leadId,
        note: payload.note.trim(),
        employeeId: auth.userId
      }
    });
  }

  async getLeadNotes(leadId: number, auth: AuthContext) {
    await this.getLeadById(leadId, auth);
    return this.prisma.leadNote.findMany({ where: { leadId }, orderBy: { id: "desc" } });
  }

  async updateLeadNote(leadId: number, noteId: number, payload: NotePayload, auth: AuthContext) {
    await this.getLeadById(leadId, auth);
    const note = await this.prisma.leadNote.findUnique({ where: { id: noteId } });

    if (!note || note.leadId !== leadId) {
      throw new HttpError(404, "Lead note not found");
    }

    if (auth.role !== "ROLE_ADMIN" && note.employeeId !== auth.userId) {
      throw new HttpError(403, "You cannot update this note");
    }

    return this.prisma.leadNote.update({
      where: { id: noteId },
      data: { note: payload.note.trim() }
    });
  }

  async deleteLeadNote(leadId: number, noteId: number, auth: AuthContext) {
    await this.getLeadById(leadId, auth);
    const note = await this.prisma.leadNote.findUnique({ where: { id: noteId } });

    if (!note || note.leadId !== leadId) {
      throw new HttpError(404, "Lead note not found");
    }

    if (auth.role !== "ROLE_ADMIN" && note.employeeId !== auth.userId) {
      throw new HttpError(403, "You cannot delete this note");
    }

    await this.prisma.leadNote.delete({ where: { id: noteId } });
  }

  async addDealNote(dealId: number, payload: NotePayload, auth: AuthContext) {
    await this.getDealById(dealId, auth);

    return this.prisma.dealNote.create({
      data: {
        dealId,
        note: payload.note.trim(),
        employeeId: auth.userId
      }
    });
  }

  async getDealNotes(dealId: number, auth: AuthContext) {
    await this.getDealById(dealId, auth);
    return this.prisma.dealNote.findMany({ where: { dealId }, orderBy: { id: "desc" } });
  }

  async updateDealNote(dealId: number, noteId: number, payload: NotePayload, auth: AuthContext) {
    await this.getDealById(dealId, auth);
    const note = await this.prisma.dealNote.findUnique({ where: { id: noteId } });

    if (!note || note.dealId !== dealId) {
      throw new HttpError(404, "Deal note not found");
    }

    if (auth.role !== "ROLE_ADMIN" && note.employeeId !== auth.userId) {
      throw new HttpError(403, "You cannot update this note");
    }

    return this.prisma.dealNote.update({
      where: { id: noteId },
      data: { note: payload.note.trim() }
    });
  }

  async deleteDealNote(dealId: number, noteId: number, auth: AuthContext) {
    await this.getDealById(dealId, auth);
    const note = await this.prisma.dealNote.findUnique({ where: { id: noteId } });

    if (!note || note.dealId !== dealId) {
      throw new HttpError(404, "Deal note not found");
    }

    if (auth.role !== "ROLE_ADMIN" && note.employeeId !== auth.userId) {
      throw new HttpError(403, "You cannot delete this note");
    }

    await this.prisma.dealNote.delete({ where: { id: noteId } });
  }

  async getAllGlobalPriorities(auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can view global priorities");
    }

    return this.prisma.priority.findMany({ orderBy: { id: "asc" } });
  }

  async createGlobalPriority(payload: PriorityPayload, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can create global priorities");
    }

    return this.prisma.priority.create({
      data: {
        status: payload.status.trim(),
        color: payload.color ?? null,
        isGlobal: true
      }
    });
  }

  async updateGlobalPriority(id: number, payload: PriorityPayload, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can update global priorities");
    }

    return this.prisma.priority.update({
      where: { id },
      data: {
        status: payload.status.trim(),
        color: payload.color ?? null
      }
    });
  }

  async deleteGlobalPriority(id: number, auth: AuthContext) {
    if (auth.role !== "ROLE_ADMIN") {
      throw new HttpError(403, "Only admins can delete global priorities");
    }

    await this.prisma.priority.delete({ where: { id } });
  }

  private ensureLeadAccess(lead: { leadOwner: string; addedBy: string }, auth: AuthContext): void {
    if (auth.role === "ROLE_ADMIN") {
      return;
    }

    if (lead.leadOwner !== auth.userId && lead.addedBy !== auth.userId) {
      throw new HttpError(403, "You don't have permission to access this lead");
    }
  }

  private async ensureDealAccess(deal: { leadId: number | null; dealAgent: string | null }, auth: AuthContext): Promise<void> {
    if (auth.role === "ROLE_ADMIN") {
      return;
    }

    if (deal.dealAgent === auth.userId) {
      return;
    }

    if (deal.leadId) {
      const lead = await this.prisma.lead.findUnique({ where: { id: deal.leadId } });
      if (lead) {
        this.ensureLeadAccess(lead, auth);
        return;
      }
    }

    throw new HttpError(403, "You don't have permission to access this deal");
  }

  private async enrichLead(lead: {
    id: number;
    name: string;
    email: string | null;
    clientCategory: string | null;
    leadSource: string | null;
    leadOwner: string;
    addedBy: string;
    status: string;
    createDeal: boolean;
    autoConvertToClient: boolean;
    companyName: string | null;
    officialWebsite: string | null;
    mobileNumber: string | null;
    officePhone: string | null;
    city: string | null;
    state: string | null;
    postalCode: string | null;
    country: string | null;
    companyAddress: string | null;
    createdAt: Date;
    updatedAt: Date;
  }) {
    return {
      ...lead,
      leadOwnerMeta: await this.employeeClient.getEmployeeMeta(lead.leadOwner),
      addedByMeta: await this.employeeClient.getEmployeeMeta(lead.addedBy)
    };
  }

  private async enrichDeal(deal: {
    id: number;
    title: string;
    value: number | null;
    currency: string | null;
    dealStage: string | null;
    dealAgent: string | null;
    dealContact: string | null;
    expectedCloseDate: Date | null;
    pipeline: string | null;
    dealCategory: string | null;
    dealWatchers: unknown;
    leadId: number | null;
    createdAt: Date;
    updatedAt: Date;
  }) {
    const watcherIds = Array.isArray(deal.dealWatchers) ? deal.dealWatchers.filter((value): value is string => typeof value === "string") : [];
    const tags = await this.prisma.dealTag.findMany({
      where: { dealId: deal.id },
      orderBy: { id: "desc" }
    });

    return {
      ...deal,
      tags,
      dealAgentMeta: deal.dealAgent ? await this.employeeClient.getEmployeeMeta(deal.dealAgent) : null,
      dealWatchersMeta: await Promise.all(watcherIds.map((watcher) => this.employeeClient.getEmployeeMeta(watcher))),
      lead: deal.leadId ? await this.prisma.lead.findUnique({ where: { id: deal.leadId } }) : null
    };
  }
}
