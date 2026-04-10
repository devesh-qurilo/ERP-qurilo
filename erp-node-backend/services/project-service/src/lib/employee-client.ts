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

    if (!response.ok) {
      throw new HttpError(404, `Employee not found: ${employeeId}`);
    }

    const data = (await response.json()) as boolean | { exists?: boolean };
    const exists = typeof data === "boolean" ? data : Boolean(data.exists);
    if (!exists) {
      throw new HttpError(404, `Employee not found: ${employeeId}`);
    }
  }

  async getEmployeeMeta(employeeId: string): Promise<EmployeeMeta | null> {
    const response = await fetch(`${this.employeeServiceUrl}/employee/meta/${encodeURIComponent(employeeId)}`);

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as EmployeeMeta;
  }

  async getEmployeesMeta(employeeIds: string[]): Promise<EmployeeMeta[]> {
    const uniqueIds = [...new Set(employeeIds.filter(Boolean))];
    const employees = await Promise.all(uniqueIds.map((employeeId) => this.getEmployeeMeta(employeeId)));
    return employees.filter((employee): employee is EmployeeMeta => employee !== null);
  }
}
