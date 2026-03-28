import { createServer } from "node:http";

import { CloudinaryStorageAdapter } from "@erp/shared-storage";
import { createLogger } from "@erp/shared-logger";

import { sendJson } from "./common/json.js";
import { getAuthConfig } from "./config/env.js";
import { getPrismaClient } from "./lib/prisma.js";
import { handleAuthRoutes } from "./modules/auth/auth.controller.js";
import { getHealthResponse } from "./modules/health/health.controller.js";
import { handleInternalRoutes } from "./modules/internal/internal.controller.js";
import { handleTranslateRoutes } from "./modules/translate/translate.controller.js";
import { PrismaRefreshTokenRepository } from "./repositories/refresh-token.repository.js";
import { PrismaUserRepository } from "./repositories/user.repository.js";
import { AuthService } from "./services/auth.service.js";
import { ConsoleMailService } from "./services/mail.service.js";
import { BcryptPasswordService } from "./services/password.service.js";
import { TokenService } from "./services/token.service.js";
import { TranslateService } from "./services/translate.service.js";

const config = getAuthConfig();
const logger = createLogger(config.serviceName);
const prisma = getPrismaClient();

const storageAdapter = new CloudinaryStorageAdapter(config.cloudinary);
void storageAdapter;

const authService = new AuthService(
  new PrismaUserRepository(prisma),
  new PrismaRefreshTokenRepository(prisma),
  new BcryptPasswordService(),
  new TokenService(config.jwtSecret),
  new ConsoleMailService(config.adminEmails)
);

const translateService = new TranslateService(config.googleTranslateApiKey);

void authService.seedDefaultAdmin();

const server = createServer(async (request, response) => {
  try {
    if (request.method === "GET" && request.url === "/health") {
      sendJson(response, 200, getHealthResponse(config.serviceName));
      return;
    }

    if (await handleInternalRoutes(request, response, authService, config.internalApiKey)) {
      return;
    }

    if (await handleAuthRoutes(request, response, authService)) {
      return;
    }

    if (await handleTranslateRoutes(request, response, translateService)) {
      return;
    }

    sendJson(response, 404, { message: "Route not found" });
  } catch (error) {
    logger.error("Unhandled auth-service error", {
      error: error instanceof Error ? error.message : "Unknown error"
    });
    sendJson(response, 500, { message: "Internal server error" });
  }
});

server.listen(config.port, () => {
  logger.info("Auth service started", {
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
