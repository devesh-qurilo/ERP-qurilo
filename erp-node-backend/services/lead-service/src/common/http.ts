import type { IncomingMessage, ServerResponse } from "node:http";

export function sendJson(response: ServerResponse, statusCode: number, payload: unknown): void {
  response.writeHead(statusCode, { "content-type": "application/json" });
  response.end(JSON.stringify(payload));
}

export async function readRawBody(request: IncomingMessage): Promise<Buffer> {
  const chunks: Buffer[] = [];

  for await (const chunk of request) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }

  return Buffer.concat(chunks);
}

export async function readJsonBody<T>(request: IncomingMessage): Promise<T> {
  const rawBody = (await readRawBody(request)).toString("utf8");
  return (rawBody ? JSON.parse(rawBody) : {}) as T;
}
