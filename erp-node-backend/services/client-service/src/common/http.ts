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

export interface MultipartFieldFile {
  filename: string | null;
  contentType: string | null;
  data: Buffer;
}

export interface MultipartFormData {
  fields: Record<string, string[]>;
  files: Record<string, MultipartFieldFile[]>;
}

export async function parseMultipartFormData(request: IncomingMessage): Promise<MultipartFormData> {
  const contentType = request.headers["content-type"] ?? "";
  const boundaryMatch = contentType.match(/boundary=(?:"([^"]+)"|([^;]+))/i);

  if (!boundaryMatch) {
    throw new Error("Missing multipart boundary");
  }

  const boundary = boundaryMatch[1] ?? boundaryMatch[2];
  const body = await readRawBody(request);
  const delimiter = Buffer.from(`--${boundary}`);
  const fields: Record<string, string[]> = {};
  const files: Record<string, MultipartFieldFile[]> = {};

  let cursor = body.indexOf(delimiter);

  while (cursor !== -1) {
    cursor += delimiter.length;

    if (body[cursor] === 45 && body[cursor + 1] === 45) {
      break;
    }

    if (body[cursor] === 13 && body[cursor + 1] === 10) {
      cursor += 2;
    }

    const nextBoundary = body.indexOf(delimiter, cursor);
    if (nextBoundary === -1) {
      break;
    }

    let part = body.subarray(cursor, nextBoundary);
    if (part.length >= 2 && part[part.length - 2] === 13 && part[part.length - 1] === 10) {
      part = part.subarray(0, part.length - 2);
    }

    const headerEnd = part.indexOf(Buffer.from("\r\n\r\n"));
    if (headerEnd === -1) {
      cursor = nextBoundary;
      continue;
    }

    const headerText = part.subarray(0, headerEnd).toString("utf8");
    const bodyData = part.subarray(headerEnd + 4);
    const disposition = headerText.match(/content-disposition:\s*form-data;\s*name="([^"]+)"(?:;\s*filename="([^"]*)")?/i);

    if (!disposition) {
      cursor = nextBoundary;
      continue;
    }

    const fieldName = disposition[1];
    const filename = disposition[2] ?? null;
    const contentTypeMatch = headerText.match(/content-type:\s*([^\r\n]+)/i);
    const partContentType = contentTypeMatch?.[1]?.trim() ?? null;

    if (filename !== null) {
      files[fieldName] ??= [];
      files[fieldName].push({ filename, contentType: partContentType, data: bodyData });
    } else {
      fields[fieldName] ??= [];
      fields[fieldName].push(bodyData.toString("utf8"));
    }

    cursor = nextBoundary;
  }

  return { fields, files };
}
