import { getBaseConfig, getString } from "@erp/shared-config";

export interface LeadConfig {
  serviceName: string;
  port: number;
  databaseUrl: string;
  jwtSecret: string;
  internalApiKey: string;
  employeeServiceUrl: string;
  clientServiceUrl: string;
  cloudinaryCloudName: string | null;
  cloudinaryApiKey: string | null;
  cloudinaryApiSecret: string | null;
  cloudinaryUploadFolder: string;
}

export function getLeadConfig(): LeadConfig {
  const base = getBaseConfig("lead-service", "LEAD_PORT", 8085);

  return {
    serviceName: base.serviceName,
    port: base.port,
    databaseUrl: getString("LEAD_DATABASE_URL"),
    jwtSecret: getString("JWT_SECRET"),
    internalApiKey: getString("INTERNAL_API_KEY"),
    employeeServiceUrl: getString("EMPLOYEE_SERVICE_URL", "http://localhost:8083").replace(/\/$/, ""),
    clientServiceUrl: getString("CLIENT_SERVICE_URL", "http://localhost:8084").replace(/\/$/, ""),
    cloudinaryCloudName: process.env.CLOUDINARY_CLOUD_NAME ?? null,
    cloudinaryApiKey: process.env.CLOUDINARY_API_KEY ?? null,
    cloudinaryApiSecret: process.env.CLOUDINARY_API_SECRET ?? null,
    cloudinaryUploadFolder: process.env.CLOUDINARY_UPLOAD_FOLDER ?? "erp"
  };
}
