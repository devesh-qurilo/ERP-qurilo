import { HttpError } from "../common/errors.js";

export interface EmployeeMeta {
  employeeId: string;
  name?: string;
  email?: string;
  role?: string;
  departmentName?: string;
  designationName?: string;
  profilePictureUrl?: string;
}

export class EmployeeClient {
  constructor(private readonly employeeServiceUrl: string) {}

  async ensureEmployeeExists(employeeId: string, authorizationHeader?: string): Promise<void> {
    const response = await fetch(`${this.employeeServiceUrl}/employee/exists/${encodeURIComponent(employeeId)}`, {
      headers: authorizationHeader ? { authorization: authorizationHeader } : {}
    });

    if (response.status === 404) {
      throw new HttpError(404, `Employee not found: ${employeeId}`);
    }

    if (!response.ok) {
      throw new HttpError(502, "Failed to validate employee");
    }

    const data = (await response.json()) as boolean | { exists?: boolean };
    const exists = typeof data === "boolean" ? data : Boolean(data.exists);

    if (!exists) {
      throw new HttpError(404, `Employee not found: ${employeeId}`);
    }
  }

  async getEmployeeMeta(employeeId: string): Promise<EmployeeMeta | null> {
    const response = await fetch(`${this.employeeServiceUrl}/employee/meta/${encodeURIComponent(employeeId)}`);

    if (response.status === 404) {
      return null;
    }

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as EmployeeMeta;
  }
}
