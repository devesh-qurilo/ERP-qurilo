import { getBaseConfig, getString } from "@erp/shared-config";

export interface LeadConfig {
  serviceName: string;
  port: number;
  databaseUrl: string;
  jwtSecret: string;
  internalApiKey: string;
  employeeServiceUrl: string;
}

export function getLeadConfig(): LeadConfig {
  const base = getBaseConfig("lead-service", "LEAD_PORT", 8085);

  return {
    serviceName: base.serviceName,
    port: base.port,
    databaseUrl: getString("LEAD_DATABASE_URL"),
    jwtSecret: getString("JWT_SECRET"),
    internalApiKey: getString("INTERNAL_API_KEY"),
    employeeServiceUrl: getString("EMPLOYEE_SERVICE_URL", "http://localhost:8083").replace(/\/$/, "")
  };
}
