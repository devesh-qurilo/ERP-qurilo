import type { IncomingMessage, ServerResponse } from "node:http";

import { isInternalRequest } from "@erp/shared-auth";

import { readJsonBody, sendJson } from "../../common/json.js";
import { AuthService, HttpError } from "../../services/auth.service.js";

async function execute(
  response: ServerResponse,
  action: () => Promise<unknown>
): Promise<void> {
  try {
    const payload = await action();
    sendJson(response, 200, payload);
  } catch (error) {
    if (error instanceof HttpError) {
      sendJson(response, error.statusCode, error.payload ?? { message: error.message });
      return;
    }

    sendJson(response, 500, { message: "Internal server error" });
  }
}

export async function handleInternalRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  authService: AuthService,
  internalApiKey: string
): Promise<boolean> {
  if (!request.url?.startsWith("/internal/auth")) {
    return false;
  }

  const requestKey = request.headers["x-internal-api-key"];
  const headerValue = Array.isArray(requestKey) ? requestKey[0] : requestKey;

  if (!isInternalRequest(headerValue, internalApiKey)) {
    sendJson(response, 401, { error: "Missing or invalid internal API key" });
    return true;
  }

  if (request.method === "GET" && request.url === "/internal/auth/health") {
    sendJson(response, 200, { status: "UP" });
    return true;
  }

  if (request.method === "POST" && request.url === "/internal/auth/register") {
    const body = await readJsonBody<Record<string, string | undefined>>(request);
    await execute(response, () => authService.registerInternal(body));
    return true;
  }

  if (request.method === "PUT" && request.url === "/internal/auth/role") {
    const body = await readJsonBody<Record<string, string | undefined>>(request);
    await execute(response, () => authService.updateRole(body));
    return true;
  }

  if (request.method === "PUT" && request.url === "/internal/auth/password") {
    const body = await readJsonBody<Record<string, string | undefined>>(request);
    await execute(response, () => authService.updatePassword(body));
    return true;
  }

  if (request.method === "PUT" && request.url === "/internal/auth/email") {
    const body = await readJsonBody<Record<string, string | undefined>>(request);
    await execute(response, () => authService.updateEmail(body));
    return true;
  }

  if (request.method === "DELETE" && request.url.startsWith("/internal/auth/")) {
    const employeeId = request.url.slice("/internal/auth/".length);
    await execute(response, () => authService.deleteUser(employeeId));
    return true;
  }

  return false;
}

