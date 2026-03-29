import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { EmployeeDocumentUploadDto } from "./dto.js";
import type { DocumentService } from "../../services/document.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleDocumentRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  documentService: DocumentService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname.match(/^\/employee\/[^/]+\/documents$/)) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.split("/")[2] ?? "";
      const body = await readJsonBody<EmployeeDocumentUploadDto>(request);
      sendJson(response, 200, await documentService.uploadDocument(employeeId, body, "ADMIN"));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/[^/]+\/documents$/)) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.split("/")[2] ?? "";
      sendJson(response, 200, await documentService.getAllDocuments(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/[^/]+\/documents\/\d+\/download$/)) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const parts = pathname.split("/");
      const employeeId = parts[2] ?? "";
      const docId = Number(parts[parts.length - 2]);
      const url = await documentService.getDownloadUrl(employeeId, docId);
      response.writeHead(302, { location: url });
      response.end();
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/[^/]+\/documents\/\d+$/)) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const parts = pathname.split("/");
      const employeeId = parts[2] ?? "";
      const docId = Number(parts.at(-1));
      sendJson(response, 200, await documentService.getDocument(employeeId, docId));
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/employee\/[^/]+\/documents\/\d+$/)) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const parts = pathname.split("/");
      const employeeId = parts[2] ?? "";
      const docId = Number(parts.at(-1));
      await documentService.deleteDocument(employeeId, docId);
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

function handleError(response: ServerResponse, error: unknown): void {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
}
