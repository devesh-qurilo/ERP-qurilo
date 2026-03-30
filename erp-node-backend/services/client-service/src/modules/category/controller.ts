import type { IncomingMessage, ServerResponse } from "node:http";

import { getAuthContext, requireAdmin } from "../../common/auth.js";
import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { ClientConfig } from "../../config/env.js";
import type { ClientService } from "../../services/client.service.js";

export async function handleCategoryRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  service: ClientService,
  config: ClientConfig
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname === "/clients/category") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      const body = await readJsonBody<{ name: string }>(request);
      sendJson(response, 200, await service.createCategory(body.name));
      return true;
    }

    if (request.method === "POST" && pathname === "/clients/category/subcategory") {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      const body = await readJsonBody<{ name: string }>(request);
      sendJson(response, 200, await service.createSubCategory(body.name));
      return true;
    }

    if (request.method === "GET" && pathname === "/clients/category") {
      sendJson(response, 200, await service.listCategories());
      return true;
    }

    if (request.method === "GET" && pathname === "/clients/category/subcategory") {
      sendJson(response, 200, await service.listSubCategories());
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/clients\/category\/\d+$/)) {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      sendJson(response, 200, await service.deleteCategory(Number(pathname.split("/").at(-1))));
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/clients\/category\/subcategory\/\d+$/)) {
      requireAdmin(getAuthContext(request.headers.authorization, config.jwtSecret));
      sendJson(response, 200, await service.deleteSubCategory(Number(pathname.split("/").at(-1))));
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
