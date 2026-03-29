import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { EmergencyContactRequestDto, UpdateEmergencyContactDto } from "./dto.js";
import type { EmergencyService } from "../../services/emergency.service.js";
import { getAuthContext } from "../../utils/auth-context.js";

export async function handleEmergencyRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  emergencyService: EmergencyService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname.match(/^\/employee\/[^/]+\/emergency-contacts$/)) {
      const employeeId = pathname.split("/")[2] ?? "";
      ensureEmergencyAccess(getAuthContext(request, jwtSecret), employeeId);
      const body = await readJsonBody<EmergencyContactRequestDto>(request);
      sendJson(response, 201, await emergencyService.createEmergencyContact(employeeId, body));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/[^/]+\/emergency-contacts$/)) {
      const employeeId = pathname.split("/")[2] ?? "";
      ensureEmergencyAccess(getAuthContext(request, jwtSecret), employeeId);
      sendJson(response, 200, await emergencyService.getAllByEmployeeId(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/[^/]+\/emergency-contacts\/\d+$/)) {
      const employeeId = pathname.split("/")[2] ?? "";
      const id = Number(pathname.split("/").at(-1));
      ensureEmergencyAccess(getAuthContext(request, jwtSecret), employeeId);
      sendJson(response, 200, await emergencyService.getByEmployeeIdAndContactId(employeeId, id));
      return true;
    }

    if (request.method === "PUT" && pathname.match(/^\/employee\/[^/]+\/emergency-contacts\/\d+$/)) {
      const employeeId = pathname.split("/")[2] ?? "";
      const id = Number(pathname.split("/").at(-1));
      ensureEmergencyAccess(getAuthContext(request, jwtSecret), employeeId);
      const body = await readJsonBody<UpdateEmergencyContactDto>(request);
      sendJson(response, 200, await emergencyService.updateEmergencyContact(employeeId, id, body));
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/employee\/[^/]+\/emergency-contacts\/\d+$/)) {
      const employeeId = pathname.split("/")[2] ?? "";
      const id = Number(pathname.split("/").at(-1));
      ensureEmergencyAccess(getAuthContext(request, jwtSecret), employeeId);
      await emergencyService.deleteByEmployeeIdAndContactId(employeeId, id);
      response.writeHead(200);
      response.end();
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

function ensureEmergencyAccess(context: { employeeId: string; roles: string[] }, employeeId: string): void {
  if (context.roles.includes("ROLE_ADMIN") || context.employeeId === employeeId.trim().toUpperCase()) {
    return;
  }

  throw new HttpError(403, "Access denied");
}

function handleError(response: ServerResponse, error: unknown): void {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
}
