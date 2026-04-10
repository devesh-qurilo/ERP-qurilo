import { createServer, type IncomingMessage } from "node:http";

import { createLogger } from "@erp/shared-logger";

import { getGatewayConfig } from "./config/env.js";
import { createRequestContext } from "./middleware/request-context.js";
import { findRouteTarget } from "./routes/route-manifest.js";

const config = getGatewayConfig();
const logger = createLogger(config.serviceName);

const server = createServer(async (req, res) => {
  const context = createRequestContext();
  const requestUrl = new URL(req.url ?? "/", "http://localhost");
  const pathname = requestUrl.pathname;
  const route = findRouteTarget(pathname, config.routes);
  const corsHeaders = getCorsHeaders(req);

  if (req.method === "OPTIONS") {
    res.writeHead(204, corsHeaders);
    res.end();
    return;
  }

  if (pathname === "/health") {
    res.writeHead(200, { ...corsHeaders, "content-type": "application/json" });
    res.end(JSON.stringify({ status: "ok", service: config.serviceName }));
    return;
  }

  if (!route) {
    logger.warn("Gateway route not found", { requestId: context.requestId, pathname });
    res.writeHead(404, { ...corsHeaders, "content-type": "application/json" });
    res.end(JSON.stringify({ message: "Route not mapped in gateway", requestId: context.requestId }));
    return;
  }

  logger.info("Gateway route resolved", {
    requestId: context.requestId,
    pathname,
    target: route.target
  });

  try {
    const upstreamUrl = resolveUpstreamUrl(requestUrl, route.target);
    const requestHeaders = new Headers();
    const skippedRequestHeaders = new Set(["host", "connection", "content-length", "transfer-encoding"]);

    for (const [key, value] of Object.entries(req.headers)) {
      if (value === undefined) {
        continue;
      }

      if (skippedRequestHeaders.has(key.toLowerCase())) {
        continue;
      }

      if (Array.isArray(value)) {
        for (const item of value) {
          requestHeaders.append(key, item);
        }
        continue;
      }

      requestHeaders.set(key, value);
    }

    requestHeaders.set("x-request-id", context.requestId);
    requestHeaders.set("x-forwarded-by", config.serviceName);

    const rawRequestBody = shouldSendBody(req.method) ? await readRawBody(req) : null;
    const requestBody =
      rawRequestBody && rawRequestBody.length > 0 ? new Uint8Array(rawRequestBody) : undefined;

    const upstreamResponse = await fetch(upstreamUrl, {
      method: req.method,
      headers: requestHeaders,
      body: requestBody,
      redirect: "manual"
    });

    const responseHeaders = new Headers(upstreamResponse.headers);
    responseHeaders.set("x-request-id", context.requestId);
    responseHeaders.delete("content-encoding");
    responseHeaders.delete("transfer-encoding");
    responseHeaders.delete("connection");
    applyCorsHeaders(responseHeaders, corsHeaders);

    res.writeHead(upstreamResponse.status, Object.fromEntries(responseHeaders.entries()));

    if (!upstreamResponse.body) {
      res.end();
      return;
    }

    const upstreamBody = Buffer.from(await upstreamResponse.arrayBuffer());
    res.end(upstreamBody);
  } catch (error) {
    logger.error("Gateway proxy forwarding failed", {
      requestId: context.requestId,
      pathname,
      target: route.target,
      error: error instanceof Error ? error.message : "Unknown error"
    });

    res.writeHead(502, { ...corsHeaders, "content-type": "application/json" });
    res.end(
      JSON.stringify({
        message: "Gateway could not reach upstream service",
        requestId: context.requestId,
        target: route.target
      })
    );
  }
});

server.listen(config.port, () => {
  logger.info("Gateway service started", { port: config.port });
});

function shouldSendBody(method: string | undefined): boolean {
  if (!method) {
    return false;
  }

  return !["GET", "HEAD"].includes(method.toUpperCase());
}

async function readRawBody(request: IncomingMessage): Promise<Buffer> {
  const chunks: Buffer[] = [];

  for await (const chunk of request) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }

  return Buffer.concat(chunks);
}

function resolveUpstreamUrl(requestUrl: URL, target: string): URL {
  const healthAliases: Record<string, string> = {
    "/auth/health": "/health",
    "/employee/health": "/health",
    "/clients/health": "/health",
    "/leads/health": "/health",
    "/projects/health": "/health",
    "/api/projects/health": "/health"
  };

  const rewrittenPath = healthAliases[requestUrl.pathname] ?? `${requestUrl.pathname}${requestUrl.search}`;
  return new URL(rewrittenPath, target);
}

function getCorsHeaders(request: IncomingMessage): Record<string, string> {
  const origin = request.headers.origin ?? "*";
  const requestedHeaders =
    typeof request.headers["access-control-request-headers"] === "string"
      ? request.headers["access-control-request-headers"]
      : "Authorization, Content-Type, X-Internal-Api-Key";

  return {
    "access-control-allow-origin": origin,
    "access-control-allow-methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
    "access-control-allow-headers": requestedHeaders,
    "access-control-expose-headers": "x-request-id",
    "access-control-max-age": "86400",
    vary: "Origin, Access-Control-Request-Headers"
  };
}

function applyCorsHeaders(headers: Headers, corsHeaders: Record<string, string>): void {
  headers.set("access-control-allow-origin", corsHeaders["access-control-allow-origin"]);
  headers.set("access-control-allow-methods", corsHeaders["access-control-allow-methods"]);
  headers.set("access-control-allow-headers", corsHeaders["access-control-allow-headers"]);
  headers.set("access-control-expose-headers", corsHeaders["access-control-expose-headers"]);
  headers.set("access-control-max-age", corsHeaders["access-control-max-age"]);
  headers.set("vary", corsHeaders.vary);
}
