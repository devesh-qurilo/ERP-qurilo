import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, readJsonBody, sendJson } from "../../common/http.js";
import type { AppreciationRequestDto } from "./dto.js";
import type { AppreciationService } from "../../services/appreciation.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleAppreciationRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  appreciationService: AppreciationService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname === "/employee/appreciations") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const payload = await readAppreciationPayload(request);
      sendJson(response, 200, await appreciationService.create(auth.employeeId, payload));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/appreciations") {
      getAuthContext(request, jwtSecret);
      sendJson(response, 200, await appreciationService.getAll());
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/appreciations\/employee\/[^/]+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      const employeeId = pathname.split("/").at(-1) ?? "";

      if (!auth.roles.includes("ROLE_ADMIN") && auth.employeeId !== employeeId.toUpperCase()) {
        throw new HttpError(403, "Not allowed");
      }

      sendJson(response, 200, await appreciationService.getForEmployee(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/appreciations\/\d+$/)) {
      getAuthContext(request, jwtSecret);
      sendJson(response, 200, await appreciationService.getById(Number(pathname.split("/").at(-1))));
      return true;
    }

    if (request.method === "PUT" && pathname.match(/^\/employee\/admin\/appreciations\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const payload = await readAppreciationPayload(request);
      sendJson(response, 200, await appreciationService.update(auth.employeeId, Number(pathname.split("/").at(-1)), payload));
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/employee\/admin\/appreciations\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      await appreciationService.delete(auth.employeeId, Number(pathname.split("/").at(-1)));
      response.writeHead(204);
      response.end();
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

async function readAppreciationPayload(request: IncomingMessage): Promise<{
  awardId?: number;
  givenToEmployeeId?: string;
  date?: string;
  summary?: string | null;
  photoUrl?: string | null;
  photoFile?: { filename: string | null; contentType: string | null; data: Buffer } | null;
}> {
  const contentType = request.headers["content-type"] ?? "";

  if (contentType.includes("multipart/form-data")) {
    const multipart = await parseMultipartFormData(request);
    return {
      awardId: multipart.fields.awardId?.[0] ? Number(multipart.fields.awardId[0]) : undefined,
      givenToEmployeeId: multipart.fields.givenToEmployeeId?.[0],
      date: multipart.fields.date?.[0],
      summary: multipart.fields.summary?.[0] ?? null,
      photoUrl: multipart.fields.photoUrl?.[0] ?? null,
      photoFile: multipart.files.photoFile?.[0] ?? null
    };
  }

  return readJsonBody<AppreciationRequestDto>(request);
}

function handleError(response: ServerResponse, error: unknown): void {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
}
