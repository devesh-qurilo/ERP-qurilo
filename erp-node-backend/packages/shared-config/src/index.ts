export type AppEnv = "development" | "test" | "production";

export interface BaseConfig {
  nodeEnv: AppEnv;
  serviceName: string;
  port: number;
}

export function getString(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;

  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }

  return value;
}

export function getNumber(name: string, fallback?: number): number {
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

export function getBaseConfig(serviceName: string, portEnvName: string, fallbackPort: number): BaseConfig {
  const rawEnv = (process.env.NODE_ENV ?? "development") as AppEnv;

  return {
    nodeEnv: rawEnv,
    serviceName,
    port: getNumber(portEnvName, fallbackPort)
  };
}

