import type { IncomingMessage, ServerResponse } from "node:http";

import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, readJsonBody, sendJson } from "../../common/http.js";
import type { EmployeeProfileUpdateDto, EmployeeRequestDto, EmployeeRoleUpdateDto } from "./dto.js";
import type { EmployeeService } from "../../services/employee.service.js";
import { getAuthContext, requireRole } from "../../utils/auth-context.js";

export async function handleEmployeeRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  employeeService: EmployeeService,
  jwtSecret: string
): Promise<boolean> {
  const pathname = new URL(request.url ?? "/", "http://localhost").pathname;

  try {
    if (request.method === "POST" && pathname === "/employee") {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const { body, profilePictureFile } = await readEmployeeRequest(request);
      sendJson(response, 200, await employeeService.createEmployee(body, profilePictureFile));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee") {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const url = new URL(request.url ?? "/", "http://localhost");
      const page = Number(url.searchParams.get("page") ?? "1");
      const pageSize = Number(url.searchParams.get("pageSize") ?? "20");
      sendJson(response, 200, await employeeService.listEmployeesPage(page, pageSize));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/all") {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN", "ROLE_EMPLOYEE");
      sendJson(response, 200, await employeeService.listEmployees());
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/me") {
      const authContext = getAuthContext(request, jwtSecret);
      sendJson(response, 200, await employeeService.getEmployee(authContext.employeeId));
      return true;
    }

    if (request.method === "PUT" && pathname === "/employee/me") {
      const authContext = getAuthContext(request, jwtSecret);
      const { body, profilePictureFile } = await readEmployeeProfileRequest(request);
      sendJson(response, 200, await employeeService.updateMyProfile(authContext.employeeId, body, profilePictureFile));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/meta/search") {
      const url = new URL(request.url ?? "/", "http://localhost");
      sendJson(response, 200, await employeeService.searchEmployeeMeta(url.searchParams.get("query") ?? ""));
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/exists/")) {
      const employeeId = pathname.replace("/employee/exists/", "");
      sendJson(response, 200, await employeeService.employeeExists(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/meta/")) {
      const employeeId = pathname.replace("/employee/meta/", "");
      sendJson(response, 200, await employeeService.getEmployeeMeta(employeeId));
      return true;
    }

    if (request.method === "GET" && pathname === "/employee/birthdays") {
      const authContext = getAuthContext(request, jwtSecret);
      requireRole(authContext, "ROLE_ADMIN", "ROLE_EMPLOYEE");
      const url = new URL(request.url ?? "/", "http://localhost");
      sendJson(response, 200, await employeeService.getEmployeesWithBirthday(url.searchParams.get("date")));
      return true;
    }

    if (request.method === "GET" && pathname.startsWith("/employee/")) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/", "");
      sendJson(response, 200, await employeeService.getEmployee(employeeId));
      return true;
    }

    if (request.method === "PUT" && pathname.startsWith("/employee/")) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/", "");
      const { body, profilePictureFile } = await readEmployeeRequest(request);
      sendJson(response, 200, await employeeService.updateEmployee(employeeId, body, profilePictureFile));
      return true;
    }

    if (request.method === "PATCH" && pathname.endsWith("/role") && pathname.startsWith("/employee/")) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/", "").replace("/role", "");
      const body = await readJsonBody<EmployeeRoleUpdateDto>(request);
      sendJson(response, 200, await employeeService.updateEmployeeRole(employeeId, body.role));
      return true;
    }

    if (request.method === "DELETE" && pathname.startsWith("/employee/")) {
      requireRole(getAuthContext(request, jwtSecret), "ROLE_ADMIN");
      const employeeId = pathname.replace("/employee/", "");
      sendJson(response, 200, await employeeService.deleteEmployee(employeeId));
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

async function readEmployeeRequest(request: IncomingMessage): Promise<{
  body: EmployeeRequestDto;
  profilePictureFile: { filename: string | null; contentType: string | null; data: Buffer } | null;
}> {
  const contentType = request.headers["content-type"] ?? "";

  if (!contentType.includes("multipart/form-data")) {
    return {
      body: await readJsonBody<EmployeeRequestDto>(request),
      profilePictureFile: null
    };
  }

  const multipart = await parseMultipartFormData(request);
  const multipartBody = parseMultipartJsonField<EmployeeRequestDto>(
    getMultipartTextValue(multipart, "employee"),
    "employee"
  );

  return {
    body: multipartBody ?? {
      employeeId: multipart.fields.employeeId?.[0],
      name: multipart.fields.name?.[0],
      email: multipart.fields.email?.[0],
      password: multipart.fields.password?.[0],
      profilePictureUrl: multipart.fields.profilePictureUrl?.[0],
      gender: multipart.fields.gender?.[0],
      birthday: multipart.fields.birthday?.[0] ?? null,
      bloodGroup: multipart.fields.bloodGroup?.[0] ?? null,
      joiningDate: multipart.fields.joiningDate?.[0] ?? null,
      language: multipart.fields.language?.[0] ?? null,
      country: multipart.fields.country?.[0] ?? null,
      mobile: multipart.fields.mobile?.[0] ?? null,
      address: multipart.fields.address?.[0] ?? null,
      about: multipart.fields.about?.[0] ?? null,
      departmentId: parseOptionalNumber(multipart.fields.departmentId?.[0]),
      designationId: parseOptionalNumber(multipart.fields.designationId?.[0]),
      reportingToId: multipart.fields.reportingToId?.[0] ?? null,
      role: multipart.fields.role?.[0] ?? null,
      loginAllowed: parseOptionalBoolean(multipart.fields.loginAllowed?.[0]),
      receiveEmailNotification: parseOptionalBoolean(multipart.fields.receiveEmailNotification?.[0]),
      hourlyRate: parseOptionalNumber(multipart.fields.hourlyRate?.[0]),
      slackMemberId: multipart.fields.slackMemberId?.[0] ?? null,
      skills: parseStringArrayField(multipart.fields.skills),
      probationEndDate: multipart.fields.probationEndDate?.[0] ?? null,
      noticePeriodStartDate: multipart.fields.noticePeriodStartDate?.[0] ?? null,
      noticePeriodEndDate: multipart.fields.noticePeriodEndDate?.[0] ?? null,
      employmentType: multipart.fields.employmentType?.[0] ?? null,
      maritalStatus: multipart.fields.maritalStatus?.[0] ?? null,
      businessAddress: multipart.fields.businessAddress?.[0] ?? null,
      officeShift: multipart.fields.officeShift?.[0] ?? null
    },
    profilePictureFile:
      multipart.files.profilePicture?.[0] ?? multipart.files.profilePictureFile?.[0] ?? multipart.files.file?.[0] ?? null
  };
}

async function readEmployeeProfileRequest(request: IncomingMessage): Promise<{
  body: EmployeeProfileUpdateDto;
  profilePictureFile: { filename: string | null; contentType: string | null; data: Buffer } | null;
}> {
  const contentType = request.headers["content-type"] ?? "";

  if (!contentType.includes("multipart/form-data")) {
    return {
      body: await readJsonBody<EmployeeProfileUpdateDto>(request),
      profilePictureFile: null
    };
  }

  const multipart = await parseMultipartFormData(request);
  const multipartBody = parseMultipartJsonField<EmployeeProfileUpdateDto>(
    getMultipartTextValue(multipart, "employee"),
    "employee"
  );

  return {
    body: multipartBody ?? {
      name: multipart.fields.name?.[0],
      email: multipart.fields.email?.[0],
      profilePictureUrl: multipart.fields.profilePictureUrl?.[0] ?? null,
      gender: multipart.fields.gender?.[0] ?? null,
      birthday: multipart.fields.birthday?.[0] ?? null,
      bloodGroup: multipart.fields.bloodGroup?.[0] ?? null,
      language: multipart.fields.language?.[0] ?? null,
      country: multipart.fields.country?.[0] ?? null,
      mobile: multipart.fields.mobile?.[0] ?? null,
      address: multipart.fields.address?.[0] ?? null,
      about: multipart.fields.about?.[0] ?? null,
      slackMemberId: multipart.fields.slackMemberId?.[0] ?? null,
      maritalStatus: multipart.fields.maritalStatus?.[0] ?? null
    },
    profilePictureFile:
      multipart.files.profilePicture?.[0] ?? multipart.files.profilePictureFile?.[0] ?? multipart.files.file?.[0] ?? null
  };
}

function parseMultipartJsonField<T>(value: string | undefined, fieldName: string): T | null {
  if (!value?.trim()) {
    return null;
  }

  try {
    return JSON.parse(value) as T;
  } catch (error) {
    throw new HttpError(400, `Invalid '${fieldName}' JSON part`, {
      message: `Invalid '${fieldName}' JSON part`,
      details: error instanceof Error ? error.message : "Malformed JSON"
    });
  }
}

function getMultipartTextValue(
  multipart: Awaited<ReturnType<typeof parseMultipartFormData>>,
  fieldName: string
): string | undefined {
  const directFieldValue = multipart.fields[fieldName]?.[0];

  if (directFieldValue !== undefined) {
    return directFieldValue;
  }

  const fileBackedField = multipart.files[fieldName]?.[0];

  if (!fileBackedField) {
    return undefined;
  }

  return fileBackedField.data.toString("utf8");
}

function parseOptionalNumber(value: string | undefined): number | null | undefined {
  if (value === undefined) {
    return undefined;
  }

  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }

  const parsed = Number(trimmed);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function parseOptionalBoolean(value: string | undefined): boolean | null | undefined {
  if (value === undefined) {
    return undefined;
  }

  const normalized = value.trim().toLowerCase();

  if (!normalized) {
    return null;
  }

  if (["true", "1", "yes", "y"].includes(normalized)) {
    return true;
  }

  if (["false", "0", "no", "n"].includes(normalized)) {
    return false;
  }

  return undefined;
}

function parseStringArrayField(values: string[] | undefined): string[] | null | undefined {
  if (values === undefined) {
    return undefined;
  }

  const joined = values.join(",");
  const trimmed = joined.trim();

  if (!trimmed) {
    return null;
  }

  if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
    try {
      const parsed = JSON.parse(trimmed) as unknown;

      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item).trim()).filter(Boolean);
      }
    } catch {
      return trimmed
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean);
    }
  }

  return trimmed
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function handleError(response: ServerResponse, error: unknown): void {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, { message: error instanceof Error ? error.message : "Internal server error" });
}
