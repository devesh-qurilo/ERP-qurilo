import { randomUUID } from "node:crypto";

export interface RequestContext {
  requestId: string;
}

export function createRequestContext(): RequestContext {
  return {
    requestId: randomUUID()
  };
}

