import { getBaseConfig, getString } from "@erp/shared-config";

export interface AuthConfig {
  serviceName: string;
  port: number;
  databaseUrl: string;
  jwtSecret: string;
  internalApiKey: string;
  adminEmails: string[];
  googleTranslateApiKey: string;
  cloudinary: {
    cloudName: string;
    apiKey: string;
    apiSecret: string;
  };
}

export function getAuthConfig(): AuthConfig {
  const base = getBaseConfig("auth-service", "AUTH_PORT", 8081);

  return {
    serviceName: base.serviceName,
    port: base.port,
    databaseUrl: getString("AUTH_DATABASE_URL"),
    jwtSecret: getString("JWT_SECRET"),
    internalApiKey: getString("INTERNAL_API_KEY"),
    adminEmails: getString("AUTH_ADMIN_EMAILS", "admin@example.com")
      .split(",")
      .map((value: string) => value.trim())
      .filter(Boolean),
    googleTranslateApiKey: getString("GOOGLE_TRANSLATE_API_KEY", "disabled"),
    cloudinary: {
      cloudName: getString("CLOUDINARY_CLOUD_NAME"),
      apiKey: getString("CLOUDINARY_API_KEY"),
      apiSecret: getString("CLOUDINARY_API_SECRET")
    }
  };
}
