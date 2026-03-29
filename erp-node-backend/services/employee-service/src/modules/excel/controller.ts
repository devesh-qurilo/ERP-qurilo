import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, sendJson } from "../../common/http.js";
import type { EmployeeService } from "../../services/employee.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleEmployeeExcelRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  employeeService: EmployeeService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname === "/employee/excel/import-csv") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const multipart = await parseMultipartFormData(request);
      const file = multipart.files.file?.[0];

      if (!file || !file.data.length) {
        throw new HttpError(400, "Empty file");
      }

      sendJson(response, 200, await employeeService.importEmployeesFromCsv(file.filename, file.data.toString("utf8")));
      return true;
    }

    if (request.method === "POST" && pathname === "/employee/excel/import-xlsx") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const multipart = await parseMultipartFormData(request);
      const file = multipart.files.file?.[0];

      if (!file || !file.data.length) {
        throw new HttpError(400, "Empty file");
      }

      sendJson(response, 200, await employeeService.importEmployeesFromXlsx(file.filename, file.data));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/excel/export") {
      const auth = getAuthContext(request, jwtSecret);
      requireRole(auth, "ROLE_ADMIN");
      const exported = await employeeService.exportEmployeesWorkbook();
      const fileName = `employees-${new Date().toISOString().replace(/[:.]/g, "-")}.xlsx`;
      response.writeHead(200, {
        "content-type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "content-disposition": `attachment; filename="${fileName}"`
      });
      response.end(exported);
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
