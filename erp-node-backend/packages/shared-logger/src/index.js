function write(level, message, context) {
    const payload = {
        timestamp: new Date().toISOString(),
        level,
        message,
        context: context ?? {}
    };
    console.log(JSON.stringify(payload));
}
export function createLogger(serviceName) {
    return {
        info(message, context) {
            write("info", message, { serviceName, ...context });
        },
        warn(message, context) {
            write("warn", message, { serviceName, ...context });
        },
        error(message, context) {
            write("error", message, { serviceName, ...context });
        }
    };
}
//# sourceMappingURL=index.js.map