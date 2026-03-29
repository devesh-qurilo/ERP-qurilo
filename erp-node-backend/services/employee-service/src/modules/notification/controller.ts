import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { SendNotificationDto, SendNotificationManyDto } from "./dto.js";
import type { NotificationService } from "../../services/notification.service.js";
import { getAuthContext, requireInternalRequest, requireRole } from "../../utils/auth-context.js";

export async function handleNotificationRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  notificationService: NotificationService,
  jwtSecret: string,
  internalApiKey: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "GET" && pathname === "/employee/notifications/me") {
      const auth = getAuthContext(request, jwtSecret);
      sendJson(response, 200, await notificationService.getMyNotifications(auth.employeeId));
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/notifications\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      sendJson(response, 200, await notificationService.getById(Number(pathname.split("/").at(-1)), auth.employeeId));
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/notifications/send") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN", "ROLE_HR");
      const body = await readJsonBody<SendNotificationDto>(request);
      await notificationService.sendNotification(auth.employeeId, body);
      sendJson(response, 200, { status: "ok" });
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/notifications/send-many") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN", "ROLE_HR");
      const body = await readJsonBody<SendNotificationManyDto>(request);
      await notificationService.sendNotificationMany(auth.employeeId, body);
      sendJson(response, 200, { status: "ok" });
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/notifications/internal/send") {
      requireInternalRequest(request, internalApiKey);
      const body = await readJsonBody<SendNotificationDto>(request);
      await notificationService.sendNotification(null, body);
      sendJson(response, 200, { status: "ok" });
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/notifications/internal/send-many") {
      requireInternalRequest(request, internalApiKey);
      const body = await readJsonBody<SendNotificationManyDto>(request);
      await notificationService.sendNotificationMany(null, body);
      sendJson(response, 200, { status: "ok" });
      return true;
    }

    if (request.method === "POST" && pathname.match(/^\/employee\/notifications\/\d+\/mark-read$/)) {
      const auth = getAuthContext(request, jwtSecret);
      await notificationService.markRead(Number(pathname.split("/")[3]), auth.employeeId);
      sendJson(response, 200, { status: "success" });
      return true;
    }

    if (request.method === "POST" && pathname.match(/^\/employee\/notifications\/\d+\/mark-unread$/)) {
      const auth = getAuthContext(request, jwtSecret);
      await notificationService.markUnread(Number(pathname.split("/")[3]), auth.employeeId);
      sendJson(response, 200, { status: "success" });
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/notifications/clear") {
      const auth = getAuthContext(request, jwtSecret);
      await notificationService.clearAll(auth.employeeId);
      sendJson(response, 200, { status: "success" });
      return true;
    }

    if (request.method === "DELETE" && pathname.match(/^\/employee\/notifications\/\d+$/)) {
      const auth = getAuthContext(request, jwtSecret);
      await notificationService.deleteById(Number(pathname.split("/").at(-1)), auth.employeeId, auth.roles.includes("ROLE_ADMIN"));
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
