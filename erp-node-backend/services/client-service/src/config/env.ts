import { getBaseConfig, getString } from "@erp/shared-config";

export interface ClientConfig {
  serviceName: string;
  port: number;
  databaseUrl: string;
  jwtSecret: string;
  internalApiKey: string;
  cloudinaryCloudName: string | null;
  cloudinaryApiKey: string | null;
  cloudinaryApiSecret: string | null;
  cloudinaryUploadFolder: string;
}

export function getClientConfig(): ClientConfig {
  const base = getBaseConfig("client-service", "CLIENT_PORT", 8084);

  return {
    serviceName: base.serviceName,
    port: base.port,
    databaseUrl: getString("CLIENT_DATABASE_URL"),
    jwtSecret: getString("JWT_SECRET"),
    internalApiKey: getString("INTERNAL_API_KEY"),
    cloudinaryCloudName: process.env.CLOUDINARY_CLOUD_NAME ?? null,
    cloudinaryApiKey: process.env.CLOUDINARY_API_KEY ?? null,
    cloudinaryApiSecret: process.env.CLOUDINARY_API_SECRET ?? null,
    cloudinaryUploadFolder: process.env.CLOUDINARY_UPLOAD_FOLDER ?? "erp"
  };
}
