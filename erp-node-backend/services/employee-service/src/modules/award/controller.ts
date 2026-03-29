import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, readJsonBody, sendJson } from "../../common/http.js";
import type { AwardRequestDto } from "./dto.js";
import type { AwardService } from "../../services/award.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleAwardRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  awardService: AwardService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname === "/employee/api/awards") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const payload = await readAwardPayload(request);
      sendJson(response, 200, await awardService.createAward(payload, auth.employeeId));
      return true;
    }

    if (request.method === "PUT" && pathname.match(/^\/employee\/api\/awards\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const awardId = Number(pathname.split("/").at(-1));
      const payload = await readAwardPayload(request);
      sendJson(response, 200, await awardService.updateAward(awardId, payload, auth.employeeId));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/awards") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      sendJson(response, 200, await awardService.getAllAwards());
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/awards/active") {
      sendJson(response, 200, await awardService.getActiveAwards());
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/api\/awards\/\d+$/)) {
      const awardId = Number(pathname.split("/").at(-1));
      const auth = tryGetAuthContext(request, jwtSecret);
      sendJson(response, 200, await awardService.getAwardById(awardId, auth?.roles.includes("ROLE_ADMIN") ?? false));
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/employee\/api\/awards\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      await awardService.deleteAward(Number(pathname.split("/").at(-1)));
      response.writeHead(204);
      response.end();
      return true;
    }

    if (request.method === "PATCH" && pathname.match(/^\/employee\/api\/awards\/\d+\/toggle-status$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const awardId = Number(pathname.split("/")[4]);
      sendJson(response, 200, await awardService.toggleAwardStatus(awardId));
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

async function readAwardPayload(request: IncomingMessage): Promise<{
  title?: string;
  summary?: string | null;
  iconUrl?: string | null;
  iconFile?: { filename: string | null; contentType: string | null; data: Buffer } | null;
}> {
  const contentType = request.headers["content-type"] ?? "";

  if (contentType.includes("multipart/form-data")) {
    const multipart = await parseMultipartFormData(request);
    return {
      title: multipart.fields.title?.[0],
      summary: multipart.fields.summary?.[0] ?? null,
      iconUrl: multipart.fields.iconUrl?.[0] ?? null,
      iconFile: multipart.files.iconFile?.[0] ?? null
    };
  }

  return readJsonBody<AwardRequestDto>(request);
}

function tryGetAuthContext(request: IncomingMessage, jwtSecret: string) {
  try {
    return getAuthContext(request, jwtSecret);
  } catch {
    return null;
  }
}

function handleError(response: ServerResponse, error: unknown): void {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
}
