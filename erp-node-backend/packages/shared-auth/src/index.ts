export interface JwtClaims {
  sub: string;
  email?: string;
  role?: string;
  type?: "access" | "refresh" | "service";
}

export interface AuthContext {
  userId: string;
  email?: string;
  role?: string;
  tokenType?: JwtClaims["type"];
}

export function getBearerToken(headerValue?: string): string | null {
  if (!headerValue) {
    return null;
  }

  if (!headerValue.startsWith("Bearer ")) {
    return null;
  }

  return headerValue.slice("Bearer ".length).trim();
}

export function isInternalRequest(internalApiKey?: string, expectedApiKey?: string): boolean {
  return Boolean(internalApiKey && expectedApiKey && internalApiKey === expectedApiKey);
}

