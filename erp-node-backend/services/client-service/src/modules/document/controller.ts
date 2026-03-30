import type { IncomingMessage, ServerResponse } from "node:http";

import { getAuthContext, requireAdmin } from "../../common/auth.js";
import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, sendJson } from "../../common/http.js";
import type { ClientConfig } from "../../config/env.js";
import type { ClientService } from "../../services/client.service.js";

export async function handleDocumentRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  service: ClientService,
  config: ClientConfig
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    const docsMatch = pathname.match(/^\/clients\/(\d+)\/documents$/);
    if (docsMatch && request.method === "POST") {
      const auth = getAuthContext(request.headers.authorization, config.jwtSecret);
      requireAdmin(auth);
      const multipart = await parseMultipartFormData(request);
      sendJson(response, 201, await service.uploadDocument(Number(docsMatch[1]), auth.userId, multipart.files.file?.[0] ?? null));
      return true;
    }

    if (docsMatch && request.method === "GET") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      sendJson(response, 200, await service.listDocuments(Number(docsMatch[1])));
      return true;
    }

    const docMatch = pathname.match(/^\/clients\/(\d+)\/documents\/(\d+)$/);
    if (docMatch && request.method === "GET") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      sendJson(response, 200, await service.getDocument(Number(docMatch[1]), Number(docMatch[2])));
      return true;
    }

    if (docMatch && request.method === "DELETE") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      await service.deleteDocument(Number(docMatch[1]), Number(docMatch[2]));
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
