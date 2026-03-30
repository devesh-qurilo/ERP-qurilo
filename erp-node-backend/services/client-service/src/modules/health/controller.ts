export function getHealthResponse(service: string): { status: string; service: string } {
  return { status: "ok", service };
}
