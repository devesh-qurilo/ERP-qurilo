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
    let categoryId: number | null = null;
    const categoryName = category?.trim() ?? "";
    const subCategoryName = subCategory?.trim() ?? "";

    if (category?.trim()) {
      const categoriesResponse = await fetch(`${this.clientServiceUrl}/clients/category`, {
        headers: this.authHeaders(authorizationHeader)
      });

      if (categoriesResponse.ok) {
        const categories = (await categoriesResponse.json()) as Array<{ id?: number; name?: string; categoryName?: string }>;
        const existing = categories.find((item) => {
          const value = item.categoryName ?? item.name ?? "";
          return value.trim().toLowerCase() === categoryName.toLowerCase();
        });

        if (existing?.id) {
          categoryId = existing.id;
        } else {
          const createdResponse = await fetch(`${this.clientServiceUrl}/clients/category`, {
            method: "POST",
            headers: {
              ...this.authHeaders(authorizationHeader),
              "content-type": "application/json"
            },
            body: JSON.stringify({ categoryName, name: categoryName })
          });

          if (createdResponse.ok) {
            const created = (await createdResponse.json()) as { id?: number };
            categoryId = created.id ?? null;
          }
        }
      }
    }

    if (subCategoryName) {
      const subCategoryUrl = categoryId
        ? `${this.clientServiceUrl}/clients/category/subcategory?categoryId=${encodeURIComponent(String(categoryId))}`
        : `${this.clientServiceUrl}/clients/category/subcategory`;
      const subCategoriesResponse = await fetch(subCategoryUrl, {
        headers: this.authHeaders(authorizationHeader)
      });

      if (subCategoriesResponse.ok) {
        const subCategories = (await subCategoriesResponse.json()) as Array<{
          name?: string;
          subCategoryName?: string;
          categoryId?: number | null;
          categoryName?: string | null;
        }>;
        const exists = subCategories.some((item) => {
          const value = item.subCategoryName ?? item.name ?? "";
          const sameName = value.trim().toLowerCase() === subCategoryName.toLowerCase();
          const sameCategory = categoryId ? item.categoryId === categoryId : true;
          return sameName && sameCategory;
        });

        if (!exists) {
          await fetch(`${this.clientServiceUrl}/clients/category/subcategory`, {
            method: "POST",
            headers: {
              ...this.authHeaders(authorizationHeader),
              "content-type": "application/json"
            },
            body: JSON.stringify({
              subCategoryName,
              name: subCategoryName,
              categoryId,
              categoryName: categoryName || undefined
            })
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
