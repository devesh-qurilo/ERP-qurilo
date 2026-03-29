import { HttpError } from "../common/errors.js";

export class ClientServiceClient {
  constructor(private readonly clientServiceUrl: string) {}

  async getClientsByEmail(email: string, authorizationHeader?: string): Promise<unknown[]> {
    const response = await fetch(`${this.clientServiceUrl}/clients/email/${encodeURIComponent(email)}`, {
      headers: authorizationHeader ? { authorization: authorizationHeader } : {}
    });

    if (response.status === 404) {
      return [];
    }

    if (!response.ok) {
      throw new HttpError(502, "Failed to check existing clients");
    }

    const payload = await response.json();
    return Array.isArray(payload) ? payload : [];
  }

  async createClientFromLead(lead: {
    name: string;
    email: string | null;
    mobileNumber: string | null;
    country: string | null;
    clientCategory: string | null;
    companyName: string | null;
    officialWebsite: string | null;
  }, authorizationHeader?: string): Promise<unknown> {
    const clientDto = {
      name: lead.name,
      email: lead.email,
      mobile: lead.mobileNumber,
      country: lead.country,
      category: lead.clientCategory,
      companyName: lead.companyName,
      website: lead.officialWebsite
    };

    const form = new FormData();
    form.append("client", JSON.stringify(clientDto));

    const response = await fetch(`${this.clientServiceUrl}/clients`, {
      method: "POST",
      headers: authorizationHeader ? { authorization: authorizationHeader } : {},
      body: form
    });

    if (!response.ok) {
      throw new HttpError(502, `Client creation failed with status ${response.status}`);
    }

    return response.json();
  }
}

export class NotificationClient {
  constructor(
    private readonly employeeServiceUrl: string,
    private readonly internalApiKey: string
  ) {}

  async send(receiverEmployeeId: string, title: string, message: string, type: string): Promise<void> {
    await fetch(`${this.employeeServiceUrl}/employee/notifications/internal/send`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-internal-api-key": this.internalApiKey
      },
      body: JSON.stringify({
        receiverEmployeeId,
        title,
        message,
        type
      })
    });
  }
}
