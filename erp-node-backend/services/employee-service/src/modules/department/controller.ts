import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { DepartmentCreateDto, DepartmentUpdateDto } from "./dto.js";
import type { EmployeeService } from "../../services/employee.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleDepartmentRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  employeeService: EmployeeService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "GET" && pathname === "/admin/departments/health") {
      sendJson(response, 200, { status: "UP" });
      return true;
    }

    if (!pathname.startsWith("/admin/departments")) {
      return false;
    }

    requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");

    if (request.method === "GET" && pathname === "/admin/departments") {
      sendJson(response, 200, await employeeService.listDepartments());
      return true;
    }

    if (request.method === "POST" && pathname === "/admin/departments") {
      const body = await readJsonBody<DepartmentCreateDto>(request);
      sendJson(response, 200, await employeeService.createDepartment(body));
      return true;
    }

    const id = Number(pathname.replace("/admin/departments/", ""));

    if (request.method === "GET") {
      sendJson(response, 200, await employeeService.getDepartment(id));
      return true;
    }

    if (request.method === "PUT") {
      const body = await readJsonBody<DepartmentUpdateDto>(request);
      sendJson(response, 200, await employeeService.updateDepartment(id, body));
      return true;
    }

    if (request.method === "DELETE") {
      sendJson(response, 200, await employeeService.deleteDepartment(id));
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
