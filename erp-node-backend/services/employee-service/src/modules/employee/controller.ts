import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { EmployeeProfileUpdateDto, EmployeeRequestDto, EmployeeRoleUpdateDto } from "./dto.js";
import type { EmployeeService } from "../../services/employee.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleEmployeeRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  employeeService: EmployeeService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname === "/employee") {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const body = await readJsonBody<EmployeeRequestDto>(request);
      sendJson(response, 200, await employeeService.createEmployee(body));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee") {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const url = new URL(request.url ?? "/", "http://localhost");
      const page = Number(url.searchParams.get("page") ?? "1");
      const pageSize = Number(url.searchParams.get("pageSize") ?? "20");
      sendJson(response, 200, await employeeService.listEmployeesPage(page, pageSize));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/all") {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN", "ROLE_EMPLOYEE");
      sendJson(response, 200, await employeeService.listEmployees());
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/me") {
      const authContext = getAuthContext(request, jwtSecret);
      sendJson(response, 200, await employeeService.getEmployee(authContext.employeeId));
      return true;
    }

    if (request.method === "PUT" && pathname === "/employee/me") {
      const authContext = getAuthContext(request, jwtSecret);
      const body = await readJsonBody<EmployeeProfileUpdateDto>(request);
      sendJson(response, 200, await employeeService.updateMyProfile(authContext.employeeId, body));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/meta/search") {
      const url = new URL(request.url ?? "/", "http://localhost");
      sendJson(response, 200, await employeeService.searchEmployeeMeta(url.searchParams.get("query") ?? ""));
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/exists/")) {
      const employeeId = pathname.replace("/employee/exists/", "");
      sendJson(response, 200, await employeeService.employeeExists(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/meta/")) {
      const employeeId = pathname.replace("/employee/meta/", "");
      sendJson(response, 200, await employeeService.getEmployeeMeta(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/birthdays") {
      const authContext = getAuthContext(request, jwtSecret);
      requireRole(authContext, "ROLE_ADMIN", "ROLE_EMPLOYEE");
      const url = new URL(request.url ?? "/", "http://localhost");
      sendJson(response, 200, await employeeService.getEmployeesWithBirthday(url.searchParams.get("date")));
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/")) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/", "");
      sendJson(response, 200, await employeeService.getEmployee(employeeId));
      return true;
    }

    if (request.method === "PUT" && pathname.startsWith("/employee/")) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/", "");
      const body = await readJsonBody<EmployeeRequestDto>(request);
      sendJson(response, 200, await employeeService.updateEmployee(employeeId, body));
      return true;
    }

    if (request.method === "PATCH" && pathname.endsWith("/role") && pathname.startsWith("/employee/")) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/", "").replace("/role", "");
      const body = await readJsonBody<EmployeeRoleUpdateDto>(request);
      sendJson(response, 200, await employeeService.updateEmployeeRole(employeeId, body.role));
      return true;
    }

    if (request.method === "DELETE" && pathname.startsWith("/employee/")) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/", "");
      sendJson(response, 200, await employeeService.deleteEmployee(employeeId));
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
