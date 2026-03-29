import type { IncomingMessage, ServerResponse } from "node:http";

import { getAuthContext } from "../../common/auth.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { LeadConfig } from "../../config/env.js";
import type { CategoryPayload, PriorityPayload, StagePayload } from "../../services/lead.service.js";
import type { LeadService } from "../../services/lead.service.js";

export async function handleAdminRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  service: LeadService,
  config: LeadConfig
): Promise<boolean> {
  const method = request.method ?? "GET";
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;
  const auth = () => getAuthContext(request.headers.authorization, config.jwtSecret);

  if (method === "POST" && pathname === "/stages") {
    const payload = await readJsonBody<StagePayload>(request);
    sendJson(response, 200, await service.createStage(payload, auth()));
    return true;
  }

  if (method === "GET" && pathname === "/stages") {
    sendJson(response, 200, await service.listStages(auth()));
    return true;
  }

  const stageMatch = pathname.match(/^\/stages\/(\d+)$/);
  if (stageMatch && method === "GET") {
    sendJson(response, 200, await service.getStage(Number(stageMatch[1]), auth()));
    return true;
  }

  if (stageMatch && method === "PUT") {
    const payload = await readJsonBody<StagePayload>(request);
    sendJson(response, 200, await service.updateStage(Number(stageMatch[1]), payload, auth()));
    return true;
  }

  if (stageMatch && method === "DELETE") {
    await service.deleteStage(Number(stageMatch[1]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  if (method === "POST" && pathname === "/deals/dealCategory") {
    const payload = await readJsonBody<CategoryPayload>(request);
    sendJson(response, 200, await service.createDealCategory(payload));
    return true;
  }

  if (method === "GET" && pathname === "/deals/dealCategory") {
    sendJson(response, 200, await service.listDealCategories());
    return true;
  }

  const dealCategoryMatch = pathname.match(/^\/deals\/dealCategory\/(\d+)$/);
  if (dealCategoryMatch && method === "DELETE") {
    sendJson(response, 200, await service.deleteDealCategory(Number(dealCategoryMatch[1])));
    return true;
  }

  if (method === "POST" && pathname === "/deals/dealCategory/LeadSource") {
    const payload = await readJsonBody<CategoryPayload>(request);
    sendJson(response, 200, await service.createLeadSource(payload));
    return true;
  }

  if (method === "GET" && pathname === "/deals/dealCategory/LeadSource") {
    sendJson(response, 200, await service.listLeadSources());
    return true;
  }

  const leadSourceMatch = pathname.match(/^\/deals\/dealCategory\/LeadSource\/(\d+)$/);
  if (leadSourceMatch && method === "DELETE") {
    sendJson(response, 200, await service.deleteLeadSource(Number(leadSourceMatch[1])));
    return true;
  }

  if (method === "GET" && pathname === "/deals/admin/priorities") {
    sendJson(response, 200, await service.getAllGlobalPriorities(auth()));
    return true;
  }

  if (method === "POST" && pathname === "/deals/admin/priorities") {
    const payload = await readJsonBody<PriorityPayload>(request);
    sendJson(response, 200, await service.createGlobalPriority(payload, auth()));
    return true;
  }

  const priorityMatch = pathname.match(/^\/deals\/admin\/priorities\/(\d+)$/);
  if (priorityMatch && method === "PUT") {
    const payload = await readJsonBody<PriorityPayload>(request);
    sendJson(response, 200, await service.updateGlobalPriority(Number(priorityMatch[1]), payload, auth()));
    return true;
  }

  if (priorityMatch && method === "DELETE") {
    await service.deleteGlobalPriority(Number(priorityMatch[1]), auth());
    response.writeHead(204);
    response.end();
    return true;
  }

  if (method === "GET" && pathname === "/deals/followups/summary") {
    sendJson(response, 200, await service.getFollowupSummary(auth()));
    return true;
  }

  return false;
}
