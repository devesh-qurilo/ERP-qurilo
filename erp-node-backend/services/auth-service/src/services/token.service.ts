import { createHmac } from "node:crypto";

export interface TokenClaims {
  sub: string;
  roles?: string[];
  type: "access" | "refresh";
  exp: number;
  iat: number;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export class TokenService {
  constructor(private readonly jwtSecret: string) {}

  generateTokenPair(employeeId: string, role: string): TokenPair {
    return {
      accessToken: this.sign({
        sub: employeeId,
        roles: [role],
        type: "access",
        iat: Date.now(),
        exp: Date.now() + 7 * 24 * 60 * 60 * 1000
      }),
      refreshToken: this.sign({
        sub: employeeId,
        type: "refresh",
        iat: Date.now(),
        exp: Date.now() + 30 * 24 * 60 * 60 * 1000
      })
    };
  }

  validateRefreshToken(token: string): TokenClaims {
    const claims = this.parse(token);

    if (claims.type !== "refresh") {
      throw new Error("Invalid refresh token type");
    }

    if (claims.exp < Date.now()) {
      throw new Error("Refresh token expired");
    }

    return claims;
  }

  private sign(claims: TokenClaims): string {
    const payload = Buffer.from(JSON.stringify(claims)).toString("base64url");
    const signature = createHmac("sha256", this.jwtSecret).update(payload).digest("base64url");
    return `${payload}.${signature}`;
  }

  private parse(token: string): TokenClaims {
    const [payload, signature] = token.split(".");

    if (!payload || !signature) {
      throw new Error("Malformed token");
    }

    const expectedSignature = createHmac("sha256", this.jwtSecret).update(payload).digest("base64url");

    if (expectedSignature !== signature) {
      throw new Error("Invalid token signature");
    }

    return JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as TokenClaims;
  }
}

