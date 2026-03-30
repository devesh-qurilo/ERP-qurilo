import type { IncomingMessage, ServerResponse } from "node:http";

import { getAuthContext, requireAdmin } from "../../common/auth.js";
import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { ClientConfig } from "../../config/env.js";
import type { ClientService } from "../../services/client.service.js";

export async function handleNoteRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  service: ClientService,
  config: ClientConfig
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    const notesMatch = pathname.match(/^\/clients\/(\d+)\/notes$/);
    if (notesMatch && request.method === "POST") {
      const auth = getAuthContext(request.headers.authorization, config.jwtSecret);
      requireAdmin(auth);
      const body = await readJsonBody<{ title: string; detail: string; type?: string | null }>(request);
      sendJson(response, 201, await service.addNote(Number(notesMatch[1]), body, auth.userId));
      return true;
    }

    if (notesMatch && request.method === "GET") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      sendJson(response, 200, await service.listNotes(Number(notesMatch[1])));
      return true;
    }

    const noteMatch = pathname.match(/^\/clients\/\d+\/notes\/(\d+)$/);
    if (noteMatch && request.method === "DELETE") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      await service.deleteNote(Number(noteMatch[1]));
      response.writeHead(204);
      response.end();
      return true;
    }

    if (noteMatch && request.method === "PUT") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      const body = await readJsonBody<{ title: string; detail: string; type?: string | null }>(request);
      sendJson(response, 202, await service.updateNote(Number(noteMatch[1]), body));
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
