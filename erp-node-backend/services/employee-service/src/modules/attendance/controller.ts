import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, readJsonBody, sendJson } from "../../common/http.js";
import type {
  AdminLeaveApplyDto,
  ImportResultDto,
  AttendancePayloadDto,
  BulkAttendanceRequestDto,
  LeaveApplyDto,
  LeaveStatusUpdateDto,
  MonthAttendanceRequestDto
} from "./dto.js";
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
    if (request.method === "POST" && pathname === "/employee/attendance/mark") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<BulkAttendanceRequestDto>(request);
      sendJson(response, 200, await attendanceLeaveService.markAttendanceForEmployees(body, auth.employeeId));
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/attendance/mark/by-employees") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<BulkAttendanceRequestDto>(request);
      sendJson(response, 200, await attendanceLeaveService.markAttendanceForEmployees(body, auth.employeeId));
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/attendance/mark/month") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<MonthAttendanceRequestDto>(request);
      sendJson(response, 200, await attendanceLeaveService.markAttendanceForMonth(body, auth.employeeId));
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/attendance/import/csv") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const multipart = await parseMultipartFormData(request);
      const uploadedFile = multipart.files.file?.[0];

      if (!uploadedFile || !uploadedFile.data.length) {
        throw new HttpError(400, "file is required");
      }

      const overwriteValue = multipart.fields.overwrite?.[0]?.trim().toLowerCase() ?? "false";
      const overwrite = overwriteValue === "true" || overwriteValue === "1" || overwriteValue === "yes";
      const results = (await attendanceLeaveService.importAttendanceFromCsv(
        uploadedFile.data.toString("utf8"),
        auth.employeeId,
        overwrite
      )) as ImportResultDto[];

      const hasFileError = results.some((result) => result.rowNumber === 0 && result.status === "ERROR");
      sendJson(response, hasFileError ? 400 : 200, results);
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/GetAllAttendance") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      sendJson(response, 200, await attendanceLeaveService.getAllSavedAttendance());
      return true;
    }

    if (request.method === "GET" && pathname.match(/^\/employee\/attendance\/\d+$/)) {
      sendJson(response, 200, await attendanceLeaveService.getAttendanceById(Number(pathname.split("/").at(-1))));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/between") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const employeeIds = url.searchParams.getAll("employeeIds");
      sendJson(
        response,
        200,
        await attendanceLeaveService.getAttendanceBetween(
          url.searchParams.get("from") ?? "",
          url.searchParams.get("to") ?? "",
          employeeIds
        )
      );
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/calendar") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_EMPLOYEE", "ROLE_ADMIN");
      const employeeId = url.searchParams.get("employeeId") || auth.employeeId;
      sendJson(
        response,
        200,
        await attendanceLeaveService.getAttendanceCalendar(
          employeeId,
          url.searchParams.get("from") ?? "",
          url.searchParams.get("to") ?? ""
        )
      );
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/exists") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_EMPLOYEE", "ROLE_ADMIN");
      const employeeId = url.searchParams.get("employeeId") || auth.employeeId;
      sendJson(response, 200, await attendanceLeaveService.attendanceExists(employeeId, url.searchParams.get("date") ?? ""));
      return true;
    }

    if (request.method === "DELETE" && pathname === "/employee/attendance/delete") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      await attendanceLeaveService.deleteAttendance(url.searchParams.get("employeeId") ?? "", url.searchParams.get("date") ?? "");
      response.writeHead(200, { "content-type": "application/json" });
      response.end(JSON.stringify({ status: "success" }));
      return true;
    }

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

    if (request.method === "GET" && pathname.match(/^\/employee\/attendance\/[^/]+\/all-saved$/)) {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/attendance/", "").replace("/all-saved", "");
      sendJson(response, 200, await attendanceLeaveService.getAllSavedAttendanceForEmployee(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/wfh") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      sendJson(response, 200, await attendanceLeaveService.getWorkFromHomeOnDate(url.searchParams.get("date") ?? ""));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/attendance/wfh/between") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      sendJson(
        response,
        200,
        await attendanceLeaveService.getWorkFromHomeBetween(
          url.searchParams.get("from") ?? "",
          url.searchParams.get("to") ?? ""
        )
      );
      return true;
    }

    if (request.method === "PATCH" && pathname === "/employee/attendance/edit") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<AttendancePayloadDto>(request);
      sendJson(
        response,
        200,
        await attendanceLeaveService.editAttendanceForEmployeeDate(
          url.searchParams.get("employeeId") ?? "",
          url.searchParams.get("date") ?? "",
          body,
          (url.searchParams.get("overwrite") ?? "true") !== "false",
          url.searchParams.get("markedBy") ?? auth.employeeId
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

    if (request.method === "POST" && pathname === "/employee/api/leaves/admin/apply") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const body = await readJsonBody<AdminLeaveApplyDto>(request);
      sendJson(response, 200, await attendanceLeaveService.applyLeavesForEmployees(body, auth.employeeId));
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
      if (pathname.match(/^\/employee\/api\/leaves\/\d+\/documents\/\d+$/)) {
        const auth = getAuthContext(request, jwtSecret);
        requireRole(auth, "ROLE_ADMIN");
        const parts = pathname.split("/");
        const leaveId = Number(parts[4]);
        const documentId = Number(parts[6]);
        await attendanceLeaveService.deleteLeaveDocument(leaveId, documentId);
        response.writeHead(204);
        response.end();
        return true;
      }

      const auth = getAuthContext(request, jwtSecret);
      const leaveId = Number(pathname.replace("/employee/api/leaves/", ""));
      await attendanceLeaveService.deleteLeave(leaveId, auth.employeeId, auth.roles.includes("ROLE_ADMIN"));
      response.writeHead(204);
      response.end();
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/api/leaves/") && pathname !== "/employee/api/leaves/calendar") {
      if (pathname.startsWith("/employee/api/leaves/employee/")) {
        const auth = getAuthContext(request, jwtSecret);
        requireRole(auth, "ROLE_ADMIN");
        const employeeId = pathname.replace("/employee/api/leaves/employee/", "");
        sendJson(response, 200, await attendanceLeaveService.getLeavesForEmployee(employeeId));
        return true;
      }

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
