import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { HolidayService } from "../../services/holiday.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";
import type { BulkHolidayRequestDto, DefaultHolidaysRequestDto, HolidayRequestDto } from "./dto.js";

export async function handleHolidayRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  holidayService: HolidayService,
  jwtSecret: string
): Promise<boolean> {
  const url = new URL(request.url ?? "/", "http://localhost");
  const pathname = url.pathname;

  try {
    if (request.method === "POST" && pathname === "/employee/api/holidays") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<HolidayRequestDto>(request);
      sendJson(response, 200, await holidayService.createHoliday(body));
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/api/holidays/bulk") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<BulkHolidayRequestDto>(request);
      sendJson(response, 200, await holidayService.createBulkHolidays(body));
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/api/holidays/default-weekly") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<DefaultHolidaysRequestDto>(request);
      sendJson(response, 200, await holidayService.setDefaultWeeklyHolidays(body));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/holidays") {
      sendJson(response, 200, await holidayService.getAllHolidays());
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/holidays/month") {
      sendJson(
        response,
        200,
        await holidayService.getHolidaysByMonth(
          Number(url.searchParams.get("year") ?? "0"),
          Number(url.searchParams.get("month") ?? "0")
        )
      );
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/holidays/upcoming") {
      sendJson(response, 200, await holidayService.getUpcomingHolidays());
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/api/holidays/")) {
      const id = Number(pathname.replace("/employee/api/holidays/", ""));
      sendJson(response, 200, await holidayService.getHolidayById(id));
      return true;
    }

    if (request.method === "PUT" && pathname.startsWith("/employee/api/holidays/")) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const id = Number(pathname.replace("/employee/api/holidays/", ""));
      const body = await readJsonBody<HolidayRequestDto>(request);
      sendJson(response, 200, await holidayService.updateHoliday(id, body));
      return true;
    }

    if (request.method === "PATCH" && pathname.startsWith("/employee/api/holidays/") && pathname.endsWith("/toggle-status")) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const id = Number(pathname.replace("/employee/api/holidays/", "").replace("/toggle-status", ""));
      await holidayService.toggleHolidayStatus(id);
      response.writeHead(204);
      response.end();
      return true;
    }

    if (request.method === "DELETE" && pathname.startsWith("/employee/api/holidays/")) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const id = Number(pathname.replace("/employee/api/holidays/", ""));
      await holidayService.deleteHoliday(id);
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
