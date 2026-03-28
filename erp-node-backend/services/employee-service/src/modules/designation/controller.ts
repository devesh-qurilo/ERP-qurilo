import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { DesignationCreateDto, DesignationUpdateDto } from "./dto.js";
import type { EmployeeService } from "../../services/employee.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleDesignationRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  employeeService: EmployeeService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (!pathname.startsWith("/admin/designations")) {
      return false;
    }

    requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");

    if (request.method === "GET" && pathname === "/admin/designations") {
      sendJson(response, 200, await employeeService.listDesignations());
      return true;
    }

    if (request.method === "POST" && pathname === "/admin/designations") {
      const body = await readJsonBody<DesignationCreateDto>(request);
      sendJson(response, 200, await employeeService.createDesignation(body));
      return true;
    }

    const id = Number(pathname.replace("/admin/designations/", ""));

    if (request.method === "GET") {
      sendJson(response, 200, await employeeService.getDesignation(id));
      return true;
    }

    if (request.method === "PUT") {
      const body = await readJsonBody<DesignationUpdateDto>(request);
      sendJson(response, 200, await employeeService.updateDesignation(id, body));
      return true;
    }

    if (request.method === "DELETE") {
      sendJson(response, 200, await employeeService.deleteDesignation(id));
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
