import { createServer } from "node:http";

import { createLogger } from "@erp/shared-logger";

import { HttpError } from "./common/errors.js";
import { sendJson } from "./common/http.js";
import { getLeadConfig } from "./config/env.js";
import { getPrismaClient } from "./lib/prisma.js";
import { EmployeeClient } from "./lib/employee-client.js";
import { handleAdminRoutes } from "./modules/admin/controller.js";
import { getHealthResponse } from "./modules/health/controller.js";
import { handleLeadRoutes } from "./modules/lead/controller.js";
import { LeadService } from "./services/lead.service.js";
import { MediaStorageService } from "./services/media-storage.service.js";

const config = getLeadConfig();
const logger = createLogger(config.serviceName);
const prisma = getPrismaClient();

const leadService = new LeadService(
  prisma,
  new EmployeeClient(config.employeeServiceUrl),
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

    if (await handleAdminRoutes(request, response, leadService, config)) {
      return;
    }

    if (await handleLeadRoutes(request, response, leadService, config)) {
      return;
    }

    sendJson(response, 404, { message: "Route not found" });
  } catch (error) {
    logger.error("Unhandled lead-service error", {
      error: error instanceof Error ? error.message : "Unknown error"
    });

    if (error instanceof HttpError) {
      sendJson(response, error.statusCode, {
        message: error.message,
        ...(error.payload ?? {})
      });
      return;
    }

    sendJson(response, 500, { message: "Internal server error" });
  }
});

server.listen(config.port, () => {
  logger.info("Lead service started", {
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
