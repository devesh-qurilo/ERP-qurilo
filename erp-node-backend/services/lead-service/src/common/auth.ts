import { createHmac } from "node:crypto";

import { getBearerToken, type AuthContext } from "@erp/shared-auth";

import { HttpError } from "./errors.js";

interface TokenClaims {
  sub: string;
  roles?: string[];
  type?: string;
  exp?: number;
}

export function getAuthContext(authorizationHeader: string | undefined, jwtSecret: string): AuthContext {
  const token = getBearerToken(authorizationHeader);

  if (!token) {
    throw new HttpError(401, "Authorization header is required");
  }

  const [payload, signature] = token.split(".");

  if (!payload || !signature) {
    throw new HttpError(401, "Malformed token");
  }

  const expectedSignature = createHmac("sha256", jwtSecret).update(payload).digest("base64url");

  if (signature !== expectedSignature) {
    throw new HttpError(401, "Invalid token signature");
  }

  const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as TokenClaims;

  if (claims.exp && claims.exp < Date.now()) {
    throw new HttpError(401, "Token expired");
  }

  return {
    userId: claims.sub,
    role: claims.roles?.[0],
    tokenType: claims.type as AuthContext["tokenType"]
  };
}

export function requireAdmin(context: AuthContext): void {
  if (context.role !== "ROLE_ADMIN") {
    throw new HttpError(403, "Admin access required");
  }
}
