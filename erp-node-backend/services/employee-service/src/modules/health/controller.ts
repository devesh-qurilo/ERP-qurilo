export function getHealthResponse(service: string): Record<string, string> {
  return {
    status: "ok",
    service
  };
}
