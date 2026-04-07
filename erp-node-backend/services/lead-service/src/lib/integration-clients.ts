import { HttpError } from "../common/errors.js";

export class ClientServiceClient {
  constructor(private readonly clientServiceUrl: string) {}

  private authHeaders(authorizationHeader?: string) {
    const headers: Record<string, string> = {};
    if (authorizationHeader) {
      headers.authorization = authorizationHeader;
    }
    return headers;
  }

  async getClientsByEmail(email: string, authorizationHeader?: string): Promise<unknown[]> {
    const response = await fetch(`${this.clientServiceUrl}/clients/email/${encodeURIComponent(email)}`, {
      headers: this.authHeaders(authorizationHeader)
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

  async ensureCategoryMetadata(category: string | null, subCategory: string | null, authorizationHeader?: string): Promise<void> {
    if (category?.trim()) {
      const categoriesResponse = await fetch(`${this.clientServiceUrl}/clients/category`, {
        headers: this.authHeaders(authorizationHeader)
      });

      if (categoriesResponse.ok) {
        const categories = (await categoriesResponse.json()) as Array<{ name?: string; categoryName?: string }>;
        const exists = categories.some((item) => {
          const value = item.categoryName ?? item.name ?? "";
          return value.trim().toLowerCase() === category.trim().toLowerCase();
        });

        if (!exists) {
          await fetch(`${this.clientServiceUrl}/clients/category`, {
            method: "POST",
            headers: {
              ...this.authHeaders(authorizationHeader),
              "content-type": "application/json"
            },
            body: JSON.stringify({ categoryName: category.trim() })
          });
        }
      }
    }

    if (subCategory?.trim()) {
      const subCategoriesResponse = await fetch(`${this.clientServiceUrl}/clients/category/subcategory`, {
        headers: this.authHeaders(authorizationHeader)
      });

      if (subCategoriesResponse.ok) {
        const subCategories = (await subCategoriesResponse.json()) as Array<{ name?: string; subCategoryName?: string }>;
        const exists = subCategories.some((item) => {
          const value = item.subCategoryName ?? item.name ?? "";
          return value.trim().toLowerCase() === subCategory.trim().toLowerCase();
        });

        if (!exists) {
          await fetch(`${this.clientServiceUrl}/clients/category/subcategory`, {
            method: "POST",
            headers: {
              ...this.authHeaders(authorizationHeader),
              "content-type": "application/json"
            },
            body: JSON.stringify({ subCategoryName: subCategory.trim() })
          });
        }
      }
    }
  }

  async createClientFromLead(lead: {
    name: string;
    email: string | null;
    mobileNumber: string | null;
    country: string | null;
    clientCategory: string | null;
    clientSubCategory: string | null;
    companyName: string | null;
    officialWebsite: string | null;
  }, authorizationHeader?: string): Promise<unknown> {
    const clientDto = {
      name: lead.name,
      email: lead.email,
      mobile: lead.mobileNumber,
      country: lead.country,
      category: lead.clientCategory,
      subCategory: lead.clientSubCategory,
      companyName: lead.companyName,
      website: lead.officialWebsite
    };

    const form = new FormData();
    form.append("client", JSON.stringify(clientDto));

    const response = await fetch(`${this.clientServiceUrl}/clients`, {
      method: "POST",
      headers: this.authHeaders(authorizationHeader),
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
