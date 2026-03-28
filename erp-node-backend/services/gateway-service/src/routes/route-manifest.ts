import type { GatewayRouteTarget } from "../config/env.js";

export function findRouteTarget(pathname: string, routes: GatewayRouteTarget[]): GatewayRouteTarget | undefined {
  return routes.find((route) => pathname === route.prefix || pathname.startsWith(`${route.prefix}/`));
}

