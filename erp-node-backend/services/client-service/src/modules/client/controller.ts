import type { IncomingMessage, ServerResponse } from "node:http";

import { getAuthContext, requireAdmin, requireEmployeeOrAdmin, requireInternalApiKey } from "../../common/auth.js";
import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, readJsonBody, sendJson } from "../../common/http.js";
import type { ClientConfig } from "../../config/env.js";
import type { ClientPayload } from "../../services/client.service.js";
import type { ClientService } from "../../services/client.service.js";

export async function handleClientRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  service: ClientService,
  config: ClientConfig
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;
  const url = new URL(request.url ?? "/", "http://localhost");

  try {
    if (request.method === "POST" && pathname === "/clients") {
      const auth = getAuthContext(request.headers.authorization, config.jwtSecret);
      requireAdmin(auth);
      const { client, profilePicture, companyLogo } = await readClientMultipartRequest(request);
      sendJson(response, 201, await service.createClient(client, auth.userId, profilePicture, companyLogo));
      return true;
    }

    if (request.method === "GET" && pathname === "/clients") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      sendJson(response, 200, await service.listClients(url.searchParams.get("search"), url.searchParams.get("status")));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/clients\/\d+$/)) {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      sendJson(response, 200, await service.getClient(Number(pathname.split("/").at(-1))));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/clients\/client\/[^/]+$/)) {
      requireEmployeeOrAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      sendJson(response, 200, await service.getClientByClientId(pathname.split("/").at(-1) ?? ""));
      return true;
    }

    if (request.method === "PUT" && pathname.match(/^\/clients\/\d+$/)) {
      const auth = getAuthContext(request.headers.authorization, config.jwtSecret);
      requireAdmin(auth);
      const { client, profilePicture, companyLogo } = await readClientMultipartRequest(request);
      sendJson(response, 200, await service.updateClient(Number(pathname.split("/").at(-1)), client, auth.userId, profilePicture, companyLogo));
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/clients\/\d+$/)) {
      const auth = getAuthContext(request.headers.authorization, config.jwtSecret);
      requireAdmin(auth);
      await service.deleteClient(Number(pathname.split("/").at(-1)));
      response.writeHead(204);
      response.end();
      return true;
    }

    if (request.method === "GET" && pathname === "/clients/internal/exists") {
      requireInternalApiKey(request.headers["x-internal-api-key"] as string | undefined, config.internalApiKey);
      sendJson(response, 200, await service.clientExistsByEmail(url.searchParams.get("email") ?? ""));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/clients\/internal\/client\/[^/]+$/)) {
      requireInternalApiKey(request.headers["x-internal-api-key"] as string | undefined, config.internalApiKey);
      sendJson(response, 200, await service.getClientByClientId(pathname.split("/").at(-1) ?? ""));
      return true;
    }

    if (request.method === "POST" && pathname === "/clients/import/csv") {
      const auth = getAuthContext(request.headers.authorization, config.jwtSecret);
      requireAdmin(auth);
      const multipart = await parseMultipartFormData(request);
      sendJson(response, 200, await service.importClientsFromCsv(multipart.files.file?.[0] ?? null, auth.userId));
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

async function readClientMultipartRequest(request: IncomingMessage): Promise<{
  client: ClientPayload;
  profilePicture: { filename: string | null; contentType: string | null; data: Buffer } | null;
  companyLogo: { filename: string | null; contentType: string | null; data: Buffer } | null;
}> {
  const contentType = request.headers["content-type"] ?? "";
  if (!contentType.includes("multipart/form-data")) {
    return {
      client: await readJsonBody<ClientPayload>(request),
      profilePicture: null,
      companyLogo: null
    };
  }

  const multipart = await parseMultipartFormData(request);
  const clientJson = multipart.fields.client?.[0];
  if (!clientJson) {
    throw new HttpError(400, "client part is required");
  }

  return {
    client: JSON.parse(clientJson) as ClientPayload,
    profilePicture: multipart.files.profilePicture?.[0] ?? null,
    companyLogo: multipart.files.companyLogo?.[0] ?? null
  };
}

function handleError(response: ServerResponse, error: unknown): void {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
}
