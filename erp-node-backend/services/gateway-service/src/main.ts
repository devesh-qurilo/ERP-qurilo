import { createServer } from "node:http";

import { createLogger } from "@erp/shared-logger";

import { getGatewayConfig } from "./config/env.js";
import { createRequestContext } from "./middleware/request-context.js";
import { findRouteTarget } from "./routes/route-manifest.js";

const config = getGatewayConfig();
const logger = createLogger(config.serviceName);

const server = createServer((req, res) => {
  const context = createRequestContext();
  const pathname = req.url ?? "/";
  const route = findRouteTarget(pathname, config.routes);

  if (pathname === "/health") {
    res.writeHead(200, { "content-type": "application/json" });
    res.end(JSON.stringify({ status: "ok", service: config.serviceName }));
    return;
  }

  if (!route) {
    logger.warn("Gateway route not found", { requestId: context.requestId, pathname });
    res.writeHead(404, { "content-type": "application/json" });
    res.end(JSON.stringify({ message: "Route not mapped in gateway", requestId: context.requestId }));
    return;
  }

  logger.info("Gateway route resolved", {
    requestId: context.requestId,
    pathname,
    target: route.target
  });

  res.writeHead(501, { "content-type": "application/json" });
  res.end(
    JSON.stringify({
      message: "Gateway proxy forwarding is scaffolded but not implemented yet",
      requestId: context.requestId,
      route
    })
  );
});

server.listen(config.port, () => {
  logger.info("Gateway service started", { port: config.port });
});

