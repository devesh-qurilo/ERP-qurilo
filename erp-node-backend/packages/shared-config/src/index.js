export function getString(name, fallback) {
    const value = process.env[name] ?? fallback;
    if (!value) {
        throw new Error(`Missing required environment variable: ${name}`);
    }
    return value;
}
export function getNumber(name, fallback) {
    const rawValue = process.env[name];
    if (!rawValue && fallback !== undefined) {
        return fallback;
    }
    if (!rawValue) {
        throw new Error(`Missing required environment variable: ${name}`);
    }
    const parsedValue = Number(rawValue);
    if (Number.isNaN(parsedValue)) {
        throw new Error(`Environment variable ${name} must be a number`);
    }
    return parsedValue;
}
export function getBaseConfig(serviceName, portEnvName, fallbackPort) {
    const rawEnv = (process.env.NODE_ENV ?? "development");
    return {
        nodeEnv: rawEnv,
        serviceName,
        port: getNumber(portEnvName, fallbackPort)
    };
}
//# sourceMappingURL=index.js.map