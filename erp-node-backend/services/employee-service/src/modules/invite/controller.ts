import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { EmployeeService } from "../../services/employee.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";
import type { CompleteRegistrationRequestDto, EmployeeInviteRequestDto } from "./dto.js";

export async function handleInviteRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  employeeService: EmployeeService,
  jwtSecret: string
): Promise<boolean> {
  const url = new URL(request.url ?? "/", "http://localhost");
  const pathname = url.pathname;

  try {
    if (request.method === "POST" && (pathname === "/employee/invite" || pathname === "/employee/invite/check")) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<EmployeeInviteRequestDto>(request);
      sendJson(response, 200, await employeeService.sendInvite(body, auth.employeeId));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/invite/check/accept") {
      sendJson(response, 200, await employeeService.acceptInvite(url.searchParams.get("token") ?? ""));
      return true;
    }

    if (request.method === "POST" && pathname === "/employeeRegister/complete-registration") {
      const authorization = request.headers.authorization;
      const body = await readJsonBody<CompleteRegistrationRequestDto>(request);
      sendJson(response, 200, await employeeService.completeRegistration(authorization, body));
      return true;
    }

    if (request.method === "PATCH" && pathname.startsWith("/employeeRegister/") && pathname.endsWith("/change-id")) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const employeeId = pathname.replace("/employeeRegister/", "").replace("/change-id", "");
      const body = await readJsonBody<{ newEmployeeId?: string }>(request);
      sendJson(response, 200, await employeeService.changeEmployeeId(employeeId, body.newEmployeeId));
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
