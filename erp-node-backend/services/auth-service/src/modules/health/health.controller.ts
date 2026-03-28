export function getHealthResponse(serviceName: string): Record<string, string> {
  return {
    status: "ok",
    service: serviceName
  };
}

