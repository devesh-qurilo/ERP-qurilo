import type { IncomingMessage, ServerResponse } from "node:http";
import { URL } from "node:url";

import { getAuthContext } from "../../common/auth.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { LeadConfig } from "../../config/env.js";
import type { LeadPayload, DealPayload, NotePayload, TagPayload } from "../../services/lead.service.js";
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

  return false;
}
