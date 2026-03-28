export function getBearerToken(headerValue) {
    if (!headerValue) {
        return null;
    }
    if (!headerValue.startsWith("Bearer ")) {
        return null;
    }
    return headerValue.slice("Bearer ".length).trim();
}
export function isInternalRequest(internalApiKey, expectedApiKey) {
    return Boolean(internalApiKey && expectedApiKey && internalApiKey === expectedApiKey);
}
//# sourceMappingURL=index.js.map