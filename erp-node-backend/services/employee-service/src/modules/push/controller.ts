import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { RegisterPushTokenRequestDto, UnregisterPushTokenRequestDto } from "./dto.js";
import type { PushService } from "../../services/push.service.js";
import { getAuthContext } from "../../utils/auth-context.js";

export async function handlePushRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  pushService: PushService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname === "/employee/push/register") {
      const auth = getAuthContext(request, jwtSecret);
      const body = await readJsonBody<RegisterPushTokenRequestDto>(request);
      await pushService.registerToken(auth.employeeId, body.provider, body.token, body.deviceInfo);
      sendJson(response, 200, { status: "ok" });
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/push/unregister") {
      const body = await readJsonBody<UnregisterPushTokenRequestDto>(request);
      await pushService.unregisterToken(body.token);
      sendJson(response, 200, { status: "ok" });
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
