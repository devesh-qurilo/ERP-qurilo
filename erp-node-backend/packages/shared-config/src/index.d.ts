export type AppEnv = "development" | "test" | "production";
export interface BaseConfig {
    nodeEnv: AppEnv;
    serviceName: string;
    port: number;
}
export declare function getString(name: string, fallback?: string): string;
export declare function getNumber(name: string, fallback?: number): number;
export declare function getBaseConfig(serviceName: string, portEnvName: string, fallbackPort: number): BaseConfig;
