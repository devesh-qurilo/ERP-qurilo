import { getBaseConfig, getString } from "@erp/shared-config";

export interface GatewayRouteTarget {
  prefix: string;
  target: string;
}

export interface GatewayConfig {
  serviceName: string;
  port: number;
  jwtSecret: string;
  internalApiKey: string;
  routes: GatewayRouteTarget[];
}

export function getGatewayConfig(): GatewayConfig {
  const base = getBaseConfig("gateway-service", "GATEWAY_PORT", 8080);

  return {
    serviceName: base.serviceName,
    port: base.port,
    jwtSecret: getString("JWT_SECRET"),
    internalApiKey: getString("INTERNAL_API_KEY"),
    routes: [
      { prefix: "/auth", target: getString("AUTH_SERVICE_URL") },
      { prefix: "/internal/auth", target: getString("AUTH_SERVICE_URL") },
      { prefix: "/employee", target: getString("EMPLOYEE_SERVICE_URL") },
      { prefix: "/admin", target: getString("EMPLOYEE_SERVICE_URL") },
      { prefix: "/clients", target: getString("CLIENT_SERVICE_URL") },
      { prefix: "/leads", target: getString("LEAD_SERVICE_URL") },
      { prefix: "/deals", target: getString("LEAD_SERVICE_URL") },
      { prefix: "/stages", target: getString("LEAD_SERVICE_URL") },
      { prefix: "/projects", target: getString("PROJECT_SERVICE_URL") },
      { prefix: "/api/projects", target: getString("PROJECT_SERVICE_URL") },
      { prefix: "/api/chat", target: getString("CHAT_SERVICE_URL") },
      { prefix: "/api/invoices", target: getString("FINANCE_SERVICE_URL") },
      { prefix: "/api/payments", target: getString("FINANCE_SERVICE_URL") },
      { prefix: "/api/payment-gateways", target: getString("FINANCE_SERVICE_URL") },
      { prefix: "/api/invoice", target: getString("FINANCE_SERVICE_URL") }
    ]
  };
}

