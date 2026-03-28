import { createHmac } from "node:crypto";
import type { IncomingMessage } from "node:http";

import { getBearerToken, isInternalRequest } from "@erp/shared-auth";

import { HttpError } from "../common/errors.js";

export interface EmployeeAuthContext {
  employeeId: string;
  roles: string[];
}

interface TokenClaims {
  sub: string;
  roles?: string[];
  type?: "access" | "refresh" | "service";
  exp: number;
}

export function getAuthContext(request: IncomingMessage, jwtSecret: string): EmployeeAuthContext {
  const token = getBearerToken(request.headers.authorization);

  if (!token) {
    throw new HttpError(401, "Authentication required");
  }

  const claims = parseToken(token, jwtSecret);

  if (claims.exp < Date.now()) {
    throw new HttpError(401, "Token expired");
  }

  return {
    employeeId: claims.sub,
    roles: claims.roles ?? []
  };
}

export function requireInternalRequest(request: IncomingMessage, expectedApiKey: string): void {
  const internalApiKey = request.headers["x-internal-api-key"];
  const providedApiKey = Array.isArray(internalApiKey) ? internalApiKey[0] : internalApiKey;

  if (!isInternalRequest(providedApiKey, expectedApiKey)) {
    throw new HttpError(403, "Forbidden");
  }
}

export function requireRole(context: EmployeeAuthContext, ...allowedRoles: string[]): void {
  if (!allowedRoles.some((role) => context.roles.includes(role))) {
    throw new HttpError(403, "Access denied");
  }
}

function parseToken(token: string, jwtSecret: string): TokenClaims {
  const [payload, signature] = token.split(".");

  if (!payload || !signature) {
    throw new HttpError(401, "Malformed token");
  }

  const expectedSignature = createHmac("sha256", jwtSecret).update(payload).digest("base64url");

  if (expectedSignature !== signature) {
    throw new HttpError(401, "Invalid token signature");
  }

  return JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as TokenClaims;
}
