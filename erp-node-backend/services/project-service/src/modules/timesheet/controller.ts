import type { IncomingMessage, ServerResponse } from "node:http";
import { URL } from "node:url";

import { getAuthContext, requireAdmin, requireEmployeeOrAdmin } from "../../common/auth.js";
import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { ProjectConfig } from "../../config/env.js";
import type { ProjectService, TimeLogPayload, WeeklyTimeLogPayload } from "../../services/project.service.js";

export async function handleTimesheetRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  service: ProjectService,
  config: ProjectConfig
): Promise<boolean> {
  const method = request.method ?? "GET";
  const url = new URL(request.url ?? "/", "http://localhost");
  const pathname = url.pathname;
  const auth = () => getAuthContext(request.headers.authorization, config.jwtSecret);

  try {
    if (method === "POST" && pathname === "/timesheets") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<TimeLogPayload>(request);
      sendJson(response, 201, await service.createTimeLog(body, context.userId));
      return true;
    }

    const timeLogByIdMatch = pathname.match(/^\/timesheets\/(\d+)$/);
    if (timeLogByIdMatch && method === "PUT") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<TimeLogPayload>(request);
      sendJson(response, 200, await service.updateTimeLog(Number(timeLogByIdMatch[1]), body, context.userId));
      return true;
    }

    if (timeLogByIdMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.deleteTimeLog(Number(timeLogByIdMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    if (method === "GET" && pathname === "/timesheets/me") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTimeLogsByEmployee(context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/timesheets") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.listAllTimeLogs());
      return true;
    }

    const timeLogsByProjectMatch = pathname.match(/^\/timesheets\/project\/(\d+)$/);
    if (timeLogsByProjectMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTimeLogsByProject(Number(timeLogsByProjectMatch[1])));
      return true;
    }

    const timeLogsByEmployeeMatch = pathname.match(/^\/timesheets\/employee\/([^/]+)$/);
    if (timeLogsByEmployeeMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      requireSelfOrAdmin(context.userId, context.role, decodeURIComponent(timeLogsByEmployeeMatch[1]));
      sendJson(response, 200, await service.listTimeLogsByEmployee(decodeURIComponent(timeLogsByEmployeeMatch[1])));
      return true;
    }

    const totalHoursByEmployeeMatch = pathname.match(/^\/timesheets\/employee\/([^/]+)\/hours$/);
    if (totalHoursByEmployeeMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      requireSelfOrAdmin(context.userId, context.role, decodeURIComponent(totalHoursByEmployeeMatch[1]));
      sendJson(response, 200, await service.getTotalHoursForEmployee(decodeURIComponent(totalHoursByEmployeeMatch[1])));
      return true;
    }

    if (method === "GET" && pathname === "/timesheets/me/day") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const date = url.searchParams.get("date");
      if (!date) {
        throw new HttpError(400, "date query param is required");
      }
      sendJson(response, 200, await service.getTimeLogsForEmployeeOnDate(context.userId, date));
      return true;
    }

    if (method === "GET" && pathname === "/timesheets/me/week") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const startDate = url.searchParams.get("startDate");
      if (!startDate) {
        throw new HttpError(400, "startDate query param is required");
      }
      sendJson(response, 200, await service.getWeekSummaryForEmployee(context.userId, startDate));
      return true;
    }

    const timeLogsByTaskMatch = pathname.match(/^\/timesheets\/task\/(\d+)$/);
    if (timeLogsByTaskMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTimeLogsByTask(Number(timeLogsByTaskMatch[1])));
      return true;
    }

    if (method === "POST" && pathname === "/timesheets/weekly") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<WeeklyTimeLogPayload>(request);
      sendJson(response, 201, await service.createWeekly(context.userId, body));
      return true;
    }

    if (method === "GET" && pathname === "/timesheets/summary") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.getAllEmployeesTimesheetSummary());
      return true;
    }

    if (method === "GET" && pathname === "/timesheets/me/summary") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getMyTimesheetSummary(context.userId));
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

function requireSelfOrAdmin(currentUserId: string, role: string | undefined, employeeId: string) {
  if (role !== "ROLE_ADMIN" && currentUserId !== employeeId) {
    throw new HttpError(403, "You can only access your own timesheet data");
  }
}

function handleError(response: ServerResponse, error: unknown) {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, {
    message: error instanceof Error ? error.message : "Internal server error"
  });
}
