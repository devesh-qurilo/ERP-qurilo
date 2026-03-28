import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { readJsonBody, sendJson } from "../../common/http.js";
import type { AttendancePayloadDto, LeaveApplyDto, LeaveStatusUpdateDto } from "./dto.js";
import type { AttendanceLeaveService } from "../../services/attendance-leave.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleAttendanceLeaveRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  attendanceLeaveService: AttendanceLeaveService,
  jwtSecret: string
): Promise<boolean> {
  const url = new URL(request.url ?? "/", "http://localhost");
  const pathname = url.pathname;

  try {
    if (request.method === "POST" && pathname === "/employee/attendance/clock/in") {
      const auth = getAuthContext(request, jwtSecret);
      const body = await readJsonBody<AttendancePayloadDto>(request);
      sendJson(response, 200, await attendanceLeaveService.clockIn(auth.employeeId, body, url.searchParams.get("date")));
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/attendance/clock/out") {
      const auth = getAuthContext(request, jwtSecret);
      const body = await readJsonBody<AttendancePayloadDto>(request);
      sendJson(response, 200, await attendanceLeaveService.clockOut(auth.employeeId, body, url.searchParams.get("date")));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/clock/activities") {
      const auth = getAuthContext(request, jwtSecret);
      sendJson(response, 200, await attendanceLeaveService.getMyActivities(auth.employeeId, url.searchParams.get("date") ?? ""));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/me") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_EMPLOYEE", "ROLE_ADMIN");
      sendJson(response, 200, await attendanceLeaveService.getMyAttendance(auth.employeeId));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/summary") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_EMPLOYEE", "ROLE_ADMIN");
      const employeeId = url.searchParams.get("employeeId") || auth.employeeId;
      sendJson(
        response,
        200,
        await attendanceLeaveService.getAttendanceSummary(
          employeeId,
          url.searchParams.get("from") ?? "",
          url.searchParams.get("to") ?? ""
        )
      );
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/api/leaves/apply") {
      const auth = getAuthContext(request, jwtSecret);
      const body = await readJsonBody<LeaveApplyDto>(request);
      sendJson(response, 200, await attendanceLeaveService.applyLeave(auth.employeeId, body));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/leaves/my-leaves") {
      const auth = getAuthContext(request, jwtSecret);
      sendJson(response, 200, await attendanceLeaveService.getMyLeaves(auth.employeeId));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/leaves") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      sendJson(response, 200, await attendanceLeaveService.getAllLeaves());
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/leaves/pending") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      sendJson(response, 200, await attendanceLeaveService.getPendingLeaves());
      return true;
    }

    if (request.method === "PATCH" && pathname.startsWith("/employee/api/leaves/") && pathname.endsWith("/status")) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const leaveId = Number(pathname.replace("/employee/api/leaves/", "").replace("/status", ""));
      const body = await readJsonBody<LeaveStatusUpdateDto>(request);
      sendJson(response, 200, await attendanceLeaveService.updateLeaveStatus(leaveId, auth.employeeId, body));
      return true;
    }

    if (request.method === "DELETE" && pathname.startsWith("/employee/api/leaves/")) {
      const auth = getAuthContext(request, jwtSecret);
      const leaveId = Number(pathname.replace("/employee/api/leaves/", ""));
      await attendanceLeaveService.deleteLeave(leaveId, auth.employeeId, auth.roles.includes("ROLE_ADMIN"));
      response.writeHead(204);
      response.end();
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/api/leaves/") && pathname !== "/employee/api/leaves/calendar") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_EMPLOYEE", "ROLE_ADMIN");
      const leaveId = Number(pathname.replace("/employee/api/leaves/", ""));
      sendJson(response, 200, await attendanceLeaveService.getLeaveById(leaveId));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/api/leaves/calendar") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_EMPLOYEE", "ROLE_ADMIN");
      sendJson(response, 200, await attendanceLeaveService.getLeaveCalendar(url.searchParams.get("date") ?? ""));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/leave-quota/me") {
      const auth = getAuthContext(request, jwtSecret);
      sendJson(response, 200, await attendanceLeaveService.getQuotasForEmployee(auth.employeeId));
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/leave-quota/employee/")) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/leave-quota/employee/", "");
      sendJson(response, 200, await attendanceLeaveService.getQuotasByEmployeeId(employeeId));
      return true;
    }
  } catch (error) {
    if (error instanceof HttpError) {
      sendJson(response, error.statusCode, error.payload ?? { message: error.message });
      return true;
    }

    sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
    return true;
  }

  return false;
}
