import { createServer } from "node:http";

import { createLogger } from "@erp/shared-logger";

import { HttpError } from "./common/errors.js";
import { sendJson } from "./common/http.js";
import { getClientConfig } from "./config/env.js";
import { getPrismaClient } from "./lib/prisma.js";
import { handleCategoryRoutes } from "./modules/category/controller.js";
import { handleClientRoutes } from "./modules/client/controller.js";
import { handleDocumentRoutes } from "./modules/document/controller.js";
import { getHealthResponse } from "./modules/health/controller.js";
import { handleNoteRoutes } from "./modules/note/controller.js";
import { ClientService } from "./services/client.service.js";
import { MediaStorageService } from "./services/media-storage.service.js";

const config = getClientConfig();
const logger = createLogger(config.serviceName);
const prisma = getPrismaClient();

const clientService = new ClientService(
  prisma,
  new MediaStorageService(
    config.cloudinaryCloudName,
    config.cloudinaryApiKey,
    config.cloudinaryApiSecret,
    config.cloudinaryUploadFolder
  )
);

const server = createServer(async (request, response) => {
  try {
    if (request.method === "GET" && request.url === "/health") {
      sendJson(response, 200, getHealthResponse(config.serviceName));
      return;
    }

    if (await handleCategoryRoutes(request, response, clientService, config)) {
      return;
    }

    if (await handleDocumentRoutes(request, response, clientService, config)) {
      return;
    }

    if (await handleNoteRoutes(request, response, clientService, config)) {
      return;
    }

    if (await handleClientRoutes(request, response, clientService, config)) {
      return;
    }

    sendJson(response, 404, { message: "Route not found" });
  } catch (error) {
    logger.error("Unhandled client-service error", {
      error: error instanceof Error ? error.message : "Unknown error"
    });
    if (error instanceof HttpError) {
      sendJson(response, error.statusCode, error.payload ?? { message: error.message });
      return;
    }
    sendJson(response, 500, { message: "Internal server error" });
  }
});

server.listen(config.port, () => {
  logger.info("Client service started", {
    port: config.port,
    databaseConfigured: Boolean(config.databaseUrl)
  });
});

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.on(signal, () => {
    void prisma.$disconnect();
    server.close();
  });
}
