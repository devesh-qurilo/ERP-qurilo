import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { CompanyRequestDto } from "./dto.js";
import type { CompanyService } from "../../services/company.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleCompanyRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  companyService: CompanyService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname === "/employee/company") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<CompanyRequestDto>(request);
      sendJson(response, 200, await companyService.createCompany(auth.employeeId, body));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/company") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN", "ROLE_EMPLOYEE");
      sendJson(response, 200, await companyService.getCompany());
      return true;
    }

    if (request.method === "PUT" && pathname === "/employee/company") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<CompanyRequestDto>(request);
      sendJson(response, 200, await companyService.updateCompany(auth.employeeId, body));
      return true;
    }

    if (request.method === "DELETE" && pathname === "/employee/company") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      await companyService.deleteCompany(auth.employeeId);
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

function handleError(response: ServerResponse, error: unknown): void {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
}
