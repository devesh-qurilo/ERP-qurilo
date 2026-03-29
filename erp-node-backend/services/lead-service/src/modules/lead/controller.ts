import type { IncomingMessage, ServerResponse } from "node:http";
import { URL } from "node:url";

import { getAuthContext } from "../../common/auth.js";
import { parseMultipartFormData, readJsonBody, sendJson } from "../../common/http.js";
import type { LeadConfig } from "../../config/env.js";
import type {
  LeadPayload,
  DealPayload,
  NotePayload,
  TagPayload,
  CommentPayload,
  DealEmployeeAssignmentPayload,
  FollowupPayload,
  FollowupUpdatePayload,
  DealDocumentUploadPayload
} from "../../services/lead.service.js";
import type { LeadService } from "../../services/lead.service.js";

export async function handleLeadRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  service: LeadService,
  config: LeadConfig
): Promise<boolean> {
  const method = request.method ?? "GET";
  const url = new URL(request.url ?? "/", "http://localhost");
  const pathname = url.pathname;
  const auth = () => getAuthContext(request.headers.authorization, config.jwtSecret);

  if (method === "POST" && pathname === "/leads") {
    const payload = await readJsonBody<LeadPayload>(request);
    sendJson(response, 200, await service.createLead(payload, auth(), request.headers.authorization));
    return true;
  }

  if (method === "POST" && pathname === "/leads/import/file") {
    const { file } = await readImportRequest(request);
    sendJson(response, 200, await service.importLeadsFromCsv(file, auth()));
    return true;
  }

  if (method === "GET" && pathname === "/leads") {
    sendJson(response, 200, await service.getAllLeads(auth()));
    return true;
  }

  if (method === "GET" && pathname === "/leads/my-leads") {
    sendJson(response, 200, await service.getMyLeads(auth()));
    return true;
  }

  const leadStatsMatch = pathname.match(/^\/leads\/(\d+)\/deal-stats$/);
  if (method === "GET" && leadStatsMatch) {
    sendJson(response, 200, await service.getLeadDealStats(Number(leadStatsMatch[1]), auth()));
    return true;
  }

  const leadByIdMatch = pathname.match(/^\/leads\/(\d+)$/);
  if (method === "GET" && leadByIdMatch) {
    sendJson(response, 200, await service.getLeadById(Number(leadByIdMatch[1]), auth()));
    return true;
  }

  if (method === "PUT" && leadByIdMatch) {
    const payload = await readJsonBody<LeadPayload>(request);
    sendJson(response, 200, await service.updateLead(Number(leadByIdMatch[1]), payload, auth(), request.headers.authorization));
    return true;
  }

  if (method === "DELETE" && leadByIdMatch) {
    await service.deleteLead(Number(leadByIdMatch[1]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  const leadNotesMatch = pathname.match(/^\/leads\/(\d+)\/notes$/);
  if (leadNotesMatch && method === "POST") {
    const payload = await readJsonBody<NotePayload>(request);
    sendJson(response, 200, await service.addLeadNote(Number(leadNotesMatch[1]), payload, auth()));
    return true;
  }

  if (leadNotesMatch && method === "GET") {
    sendJson(response, 200, await service.getLeadNotes(Number(leadNotesMatch[1]), auth()));
    return true;
  }

  const leadNoteByIdMatch = pathname.match(/^\/leads\/(\d+)\/notes\/(\d+)$/);
  if (leadNoteByIdMatch && method === "PUT") {
    const payload = await readJsonBody<NotePayload>(request);
    sendJson(response, 200, await service.updateLeadNote(Number(leadNoteByIdMatch[1]), Number(leadNoteByIdMatch[2]), payload, auth()));
    return true;
  }

  if (leadNoteByIdMatch && method === "DELETE") {
    await service.deleteLeadNote(Number(leadNoteByIdMatch[1]), Number(leadNoteByIdMatch[2]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  if (method === "POST" && pathname === "/deals") {
    const payload = await readJsonBody<DealPayload>(request);
    sendJson(response, 200, await service.createDeal(payload, auth(), request.headers.authorization));
    return true;
  }

  if (method === "POST" && pathname === "/deals/import/csv") {
    const { file } = await readImportRequest(request);
    sendJson(response, 200, await service.importDealsFromCsv(file, auth()));
    return true;
  }

  if (method === "GET" && pathname === "/deals") {
    sendJson(response, 200, await service.getAllDeals(auth()));
    return true;
  }

  if (method === "GET" && pathname === "/deals/stats") {
    sendJson(response, 200, await service.getGlobalDealStats(auth()));
    return true;
  }

  const dealsByLeadMatch = pathname.match(/^\/deals\/lead\/(\d+)$/);
  if (method === "GET" && dealsByLeadMatch) {
    sendJson(response, 200, await service.getDealsByLeadId(Number(dealsByLeadMatch[1]), auth()));
    return true;
  }

  const dealByIdMatch = pathname.match(/^\/deals\/(\d+)$/);
  if (method === "GET" && dealByIdMatch) {
    sendJson(response, 200, await service.getDealById(Number(dealByIdMatch[1]), auth()));
    return true;
  }

  if (method === "PUT" && dealByIdMatch) {
    const payload = await readJsonBody<DealPayload>(request);
    sendJson(response, 200, await service.updateDeal(Number(dealByIdMatch[1]), payload, auth(), request.headers.authorization));
    return true;
  }

  if (method === "DELETE" && dealByIdMatch) {
    await service.deleteDeal(Number(dealByIdMatch[1]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  const dealStageMatch = pathname.match(/^\/deals\/(\d+)\/stage$/);
  if (method === "PUT" && dealStageMatch) {
    const stage = url.searchParams.get("stage");
    if (!stage) {
      sendJson(response, 400, { message: "stage query param is required" });
      return true;
    }
    sendJson(response, 200, await service.updateDealStage(Number(dealStageMatch[1]), stage, auth()));
    return true;
  }

  const dealNotesMatch = pathname.match(/^\/deals\/(\d+)\/notes$/);
  if (dealNotesMatch && method === "POST") {
    const payload = await readJsonBody<NotePayload>(request);
    sendJson(response, 200, await service.addDealNote(Number(dealNotesMatch[1]), payload, auth()));
    return true;
  }

  if (dealNotesMatch && method === "GET") {
    sendJson(response, 200, await service.getDealNotes(Number(dealNotesMatch[1]), auth()));
    return true;
  }

  const dealNoteByIdMatch = pathname.match(/^\/deals\/(\d+)\/notes\/(\d+)$/);
  if (dealNoteByIdMatch && method === "PUT") {
    const payload = await readJsonBody<NotePayload>(request);
    sendJson(response, 200, await service.updateDealNote(Number(dealNoteByIdMatch[1]), Number(dealNoteByIdMatch[2]), payload, auth()));
    return true;
  }

  if (dealNoteByIdMatch && method === "DELETE") {
    await service.deleteDealNote(Number(dealNoteByIdMatch[1]), Number(dealNoteByIdMatch[2]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  const dealTagsMatch = pathname.match(/^\/deals\/(\d+)\/tags$/);
  if (dealTagsMatch && method === "GET") {
    sendJson(response, 200, await service.getDealTags(Number(dealTagsMatch[1]), auth()));
    return true;
  }

  if (dealTagsMatch && method === "POST") {
    const payload = await readJsonBody<TagPayload>(request);
    await service.addDealTag(Number(dealTagsMatch[1]), payload, auth());
    sendJson(response, 200, { message: "Success" });
    return true;
  }

  const dealTagByIdMatch = pathname.match(/^\/deals\/(\d+)\/tags\/(\d+)$/);
  if (dealTagByIdMatch && method === "DELETE") {
    await service.deleteDealTag(Number(dealTagByIdMatch[1]), Number(dealTagByIdMatch[2]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  const dealCommentsMatch = pathname.match(/^\/deals\/(\d+)\/comments$/);
  if (dealCommentsMatch && method === "GET") {
    sendJson(response, 200, await service.getDealComments(Number(dealCommentsMatch[1]), auth()));
    return true;
  }

  if (dealCommentsMatch && method === "POST") {
    const payload = await readJsonBody<CommentPayload>(request);
    sendJson(response, 200, await service.addDealComment(Number(dealCommentsMatch[1]), payload, auth()));
    return true;
  }

  const dealCommentByIdMatch = pathname.match(/^\/deals\/(\d+)\/comments\/(\d+)$/);
  if (dealCommentByIdMatch && method === "PUT") {
    const payload = await readJsonBody<CommentPayload>(request);
    sendJson(response, 200, await service.updateDealComment(Number(dealCommentByIdMatch[1]), Number(dealCommentByIdMatch[2]), payload, auth()));
    return true;
  }

  if (dealCommentByIdMatch && method === "DELETE") {
    await service.deleteDealComment(Number(dealCommentByIdMatch[1]), Number(dealCommentByIdMatch[2]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  const dealEmployeesMatch = pathname.match(/^\/deals\/(\d+)\/employees$/);
  if (dealEmployeesMatch && method === "GET") {
    sendJson(response, 200, await service.listDealEmployees(Number(dealEmployeesMatch[1]), auth()));
    return true;
  }

  if (dealEmployeesMatch && method === "POST") {
    const payload = await readJsonBody<DealEmployeeAssignmentPayload>(request);
    await service.assignDealEmployees(Number(dealEmployeesMatch[1]), payload, auth(), request.headers.authorization);
    response.writeHead(200);
    response.end();
    return true;
  }

  const dealEmployeeByIdMatch = pathname.match(/^\/deals\/(\d+)\/employees\/([^/]+)$/);
  if (dealEmployeeByIdMatch && method === "DELETE") {
    await service.removeDealEmployee(Number(dealEmployeeByIdMatch[1]), decodeURIComponent(dealEmployeeByIdMatch[2]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  const dealFollowupsMatch = pathname.match(/^\/deals\/(\d+)\/followups$/);
  if (dealFollowupsMatch && method === "GET") {
    sendJson(response, 200, await service.listDealFollowups(Number(dealFollowupsMatch[1]), auth()));
    return true;
  }

  if (dealFollowupsMatch && method === "POST") {
    const payload = await readJsonBody<FollowupPayload>(request);
    sendJson(response, 200, await service.addDealFollowup(Number(dealFollowupsMatch[1]), payload, auth()));
    return true;
  }

  const dealFollowupByIdMatch = pathname.match(/^\/deals\/(\d+)\/followups\/(\d+)$/);
  if (dealFollowupByIdMatch && method === "GET") {
    sendJson(response, 200, await service.getDealFollowup(Number(dealFollowupByIdMatch[1]), Number(dealFollowupByIdMatch[2]), auth()));
    return true;
  }

  if (dealFollowupByIdMatch && method === "PUT") {
    const payload = await readJsonBody<FollowupUpdatePayload>(request);
    sendJson(response, 200, await service.updateDealFollowup(Number(dealFollowupByIdMatch[1]), Number(dealFollowupByIdMatch[2]), payload, auth()));
    return true;
  }

  if (dealFollowupByIdMatch && method === "DELETE") {
    await service.deleteDealFollowup(Number(dealFollowupByIdMatch[1]), Number(dealFollowupByIdMatch[2]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  const dealDocumentsMatch = pathname.match(/^\/deals\/(\d+)\/documents$/);
  if (dealDocumentsMatch && method === "GET") {
    sendJson(response, 200, await service.listDealDocuments(Number(dealDocumentsMatch[1]), auth()));
    return true;
  }

  if (dealDocumentsMatch && method === "POST") {
    const { body, file } = await readDealDocumentRequest(request);
    sendJson(response, 200, await service.uploadDealDocument(Number(dealDocumentsMatch[1]), body, auth(), file));
    return true;
  }

  const dealDocumentByIdMatch = pathname.match(/^\/deals\/(\d+)\/documents\/(\d+)$/);
  if (dealDocumentByIdMatch && method === "GET") {
    sendJson(response, 200, await service.getDealDocument(Number(dealDocumentByIdMatch[1]), Number(dealDocumentByIdMatch[2]), auth()));
    return true;
  }

  if (dealDocumentByIdMatch && method === "DELETE") {
    await service.deleteDealDocument(Number(dealDocumentByIdMatch[1]), Number(dealDocumentByIdMatch[2]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  return false;
}

async function readDealDocumentRequest(request: IncomingMessage): Promise<{
  body: DealDocumentUploadPayload;
  file: { filename: string | null; contentType: string | null; data: Buffer } | null;
}> {
  const contentType = request.headers["content-type"] ?? "";

  if (!contentType.includes("multipart/form-data")) {
    return {
      body: await readJsonBody<DealDocumentUploadPayload>(request),
      file: null
    };
  }

  const multipart = await parseMultipartFormData(request);

  return {
    body: {
      filename: multipart.fields.filename?.[0] ?? undefined,
      url: multipart.fields.url?.[0] ?? undefined
    },
    file: multipart.files.file?.[0] ?? multipart.files.document?.[0] ?? null
  };
}

async function readImportRequest(request: IncomingMessage): Promise<{
  file: { filename: string | null; contentType: string | null; data: Buffer } | null;
}> {
  const contentType = request.headers["content-type"] ?? "";

  if (!contentType.includes("multipart/form-data")) {
    return { file: null };
  }

  const multipart = await parseMultipartFormData(request);

  return {
    file: multipart.files.file?.[0] ?? null
  };
}
