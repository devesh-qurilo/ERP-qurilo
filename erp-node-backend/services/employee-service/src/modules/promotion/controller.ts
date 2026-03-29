import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { PromotionRequestDto } from "./dto.js";
import type { PromotionService } from "../../services/promotion.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handlePromotionRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  promotionService: PromotionService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname.match(/^\/admin\/api\/promotions\/employee\/[^/]+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const employeeId = pathname.split("/").at(-1) ?? "";
      const body = await readJsonBody<PromotionRequestDto>(request);
      sendJson(response, 200, await promotionService.createPromotion(employeeId, body, auth.employeeId));
      return true;
    }

    if (request.method === "GET" && pathname === "/admin/api/promotions") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      sendJson(response, 200, await promotionService.getAllPromotions());
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/admin\/api\/promotions\/employee\/[^/]+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const employeeId = pathname.split("/").at(-1) ?? "";
      sendJson(response, 200, await promotionService.getPromotionsByEmployee(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/admin\/api\/promotions\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      sendJson(response, 200, await promotionService.getPromotionById(Number(pathname.split("/").at(-1))));
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/admin\/api\/promotions\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      await promotionService.deletePromotion(Number(pathname.split("/").at(-1)));
      response.writeHead(204);
      response.end();
      return true;
    }

    if (request.method === "PUT" && pathname.match(/^\/admin\/api\/promotions\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<PromotionRequestDto>(request);
      sendJson(response, 200, await promotionService.updatePromotion(Number(pathname.split("/").at(-1)), body, auth.employeeId));
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

function handleError(response: ServerResponse, error: unknown): void {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
}
