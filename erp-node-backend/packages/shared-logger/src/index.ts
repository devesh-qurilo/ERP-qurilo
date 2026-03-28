export interface Logger {
  info(message: string, context?: Record<string, unknown>): void;
  warn(message: string, context?: Record<string, unknown>): void;
  error(message: string, context?: Record<string, unknown>): void;
}

function write(level: string, message: string, context?: Record<string, unknown>): void {
  const payload = {
    timestamp: new Date().toISOString(),
    level,
    message,
    context: context ?? {}
  };

  console.log(JSON.stringify(payload));
}

export function createLogger(serviceName: string): Logger {
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

