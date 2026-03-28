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
export declare function getBearerToken(headerValue?: string): string | null;
export declare function isInternalRequest(internalApiKey?: string, expectedApiKey?: string): boolean;
