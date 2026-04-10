import { createServer } from "node:http";

import { createLogger } from "@erp/shared-logger";

import { HttpError } from "./common/errors.js";
import { sendJson } from "./common/http.js";
import { getProjectConfig } from "./config/env.js";
import { ClientClient } from "./lib/client-client.js";
import { EmployeeClient } from "./lib/employee-client.js";
import { getPrismaClient } from "./lib/prisma.js";
import { handleDiscussionRoutes } from "./modules/discussion/controller.js";
import { getHealthResponse } from "./modules/health/controller.js";
import { handleProjectRoutes } from "./modules/project/controller.js";
import { handleTimesheetRoutes } from "./modules/timesheet/controller.js";
import { MediaStorageService } from "./services/media-storage.service.js";
import { ProjectService } from "./services/project.service.js";

const config = getProjectConfig();
const logger = createLogger(config.serviceName);
const prisma = getPrismaClient();

const projectService = new ProjectService(
  prisma,
  new EmployeeClient(config.employeeServiceUrl),
  new ClientClient(config.clientServiceUrl, config.internalApiKey),
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

    if (await handleTimesheetRoutes(request, response, projectService, config)) {
      return;
    }

    if (await handleDiscussionRoutes(request, response, projectService, config)) {
      return;
    }

    if (await handleProjectRoutes(request, response, projectService, config)) {
      return;
    }

    sendJson(response, 404, { message: "Route not found" });
  } catch (error) {
    logger.error("Unhandled project-service error", {
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
  logger.info("Project service started", {
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
