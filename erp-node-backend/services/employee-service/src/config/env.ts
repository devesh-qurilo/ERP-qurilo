import { getBaseConfig, getString } from "@erp/shared-config";

export interface EmployeeConfig {
  serviceName: string;
  port: number;
  databaseUrl: string;
  jwtSecret: string;
  internalApiKey: string;
  authServiceUrl: string;
}

export function getEmployeeConfig(): EmployeeConfig {
  const base = getBaseConfig("employee-service", "EMPLOYEE_PORT", 8083);

  return {
    serviceName: base.serviceName,
    port: base.port,
    databaseUrl: getString("EMPLOYEE_DATABASE_URL"),
    jwtSecret: getString("JWT_SECRET"),
    internalApiKey: getString("INTERNAL_API_KEY"),
    authServiceUrl: getString("AUTH_SERVICE_URL", "http://localhost:8081").replace(/\/$/, "")
  };
}
