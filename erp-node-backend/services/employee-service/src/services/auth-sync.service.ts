export class AuthSyncService {
  constructor(
    private readonly authServiceUrl: string,
    private readonly internalApiKey: string
  ) {}

  async register(employeeId: string, password: string, role: string, email: string): Promise<void> {
    await this.request("/internal/auth/register", "POST", { employeeId, password, role, email });
  }

  async updateRole(employeeId: string, role: string): Promise<void> {
    await this.request("/internal/auth/role", "PUT", { employeeId, role });
  }

  async updatePassword(employeeId: string, password: string): Promise<void> {
    await this.request("/internal/auth/password", "PUT", { employeeId, password });
  }

  async updateEmail(employeeId: string, email: string): Promise<void> {
    await this.request("/internal/auth/email", "PUT", { employeeId, email });
  }

  async delete(employeeId: string): Promise<void> {
    const response = await fetch(`${this.authServiceUrl}/internal/auth/${employeeId}`, {
      method: "DELETE",
      headers: {
        "x-internal-api-key": this.internalApiKey
      }
    });

    if (!response.ok) {
      throw new Error(`Auth delete failed with ${response.status}`);
    }
  }

  private async request(pathname: string, method: string, body: Record<string, unknown>): Promise<void> {
    const response = await fetch(`${this.authServiceUrl}${pathname}`, {
      method,
      headers: {
        "content-type": "application/json",
        "x-internal-api-key": this.internalApiKey
      },
      body: JSON.stringify(body)
    });

    if (!response.ok) {
      const payload = await response.text();
      throw new Error(`Auth sync failed with ${response.status}: ${payload}`);
    }
  }
}
