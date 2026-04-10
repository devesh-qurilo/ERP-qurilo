import { HttpError } from "../common/errors.js";

export interface ClientMeta {
  id?: number;
  clientId?: string;
  name?: string;
  companyName?: string;
  email?: string;
  mobile?: string;
  profilePictureUrl?: string;
  companyLogoUrl?: string;
}

export class ClientClient {
  constructor(private readonly clientServiceUrl: string, private readonly internalApiKey: string) {}

  async getClientByClientId(clientId: string): Promise<ClientMeta | null> {
    const response = await fetch(`${this.clientServiceUrl}/clients/internal/client/${encodeURIComponent(clientId)}`, {
      headers: {
        "x-internal-api-key": this.internalApiKey
      }
    });

    if (response.status === 404) {
      return null;
    }

    if (!response.ok) {
      throw new HttpError(502, "Failed to load client metadata");
    }

    return (await response.json()) as ClientMeta;
  }
}
