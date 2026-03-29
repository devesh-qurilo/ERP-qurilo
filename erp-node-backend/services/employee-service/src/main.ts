import { createServer } from "node:http";

import { createLogger } from "@erp/shared-logger";

import { handleAttendanceLeaveRoutes } from "./modules/attendance/controller.js";
import { sendJson } from "./common/http.js";
import { getEmployeeConfig } from "./config/env.js";
import { getPrismaClient } from "./lib/prisma.js";
import { handleCompanyRoutes } from "./modules/company/controller.js";
import { handleDepartmentRoutes } from "./modules/department/controller.js";
import { handleDesignationRoutes } from "./modules/designation/controller.js";
import { handleEmergencyRoutes } from "./modules/emergency/controller.js";
import { handleEmployeeRoutes } from "./modules/employee/controller.js";
import { getHealthResponse } from "./modules/health/controller.js";
import { handleHolidayRoutes } from "./modules/holiday/controller.js";
import { handleInviteRoutes } from "./modules/invite/controller.js";
import { AttendanceLeaveService } from "./services/attendance-leave.service.js";
import { AuthSyncService } from "./services/auth-sync.service.js";
import { CompanyService } from "./services/company.service.js";
import { EmergencyService } from "./services/emergency.service.js";
import { EmployeeService } from "./services/employee.service.js";
import { HolidayService } from "./services/holiday.service.js";

const config = getEmployeeConfig();
const logger = createLogger(config.serviceName);
const prisma = getPrismaClient();

const attendanceLeaveService = new AttendanceLeaveService(prisma);
const companyService = new CompanyService(prisma);
const emergencyService = new EmergencyService(prisma);
const holidayService = new HolidayService(prisma);
const employeeService = new EmployeeService(
  prisma,
  new AuthSyncService(config.authServiceUrl, config.internalApiKey),
  config.jwtSecret,
  attendanceLeaveService
);

const server = createServer(async (request, response) => {
  try {
    if (request.method === "GET" && request.url === "/health") {
      sendJson(response, 200, getHealthResponse(config.serviceName));
      return;
    }

    if (await handleDepartmentRoutes(request, response, employeeService, config.jwtSecret)) {
      return;
    }

    if (await handleDesignationRoutes(request, response, employeeService, config.jwtSecret)) {
      return;
    }

    if (await handleAttendanceLeaveRoutes(request, response, attendanceLeaveService, config.jwtSecret)) {
      return;
    }

    if (await handleCompanyRoutes(request, response, companyService, config.jwtSecret)) {
      return;
    }

    if (await handleEmergencyRoutes(request, response, emergencyService, config.jwtSecret)) {
      return;
    }

    if (await handleHolidayRoutes(request, response, holidayService, config.jwtSecret)) {
      return;
    }

    if (await handleInviteRoutes(request, response, employeeService, config.jwtSecret)) {
      return;
    }

    if (await handleEmployeeRoutes(request, response, employeeService, config.jwtSecret)) {
      return;
    }

    sendJson(response, 404, { message: "Route not found" });
  } catch (error) {
    logger.error("Unhandled employee-service error", {
      error: error instanceof Error ? error.message : "Unknown error"
    });
    sendJson(response, 500, { message: "Internal server error" });
  }
});

server.listen(config.port, () => {
  logger.info("Employee service started", {
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
