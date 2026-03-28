import type { IncomingMessage, ServerResponse } from "node:http";

import { readJsonBody, sendJson } from "../../common/json.js";
import type {
  ForgotTicketRequestDto,
  LoginRequestDto,
  ManualRegisterRequestDto,
  OtpRequestDto,
  OtpVerifyRequestDto,
  RefreshRequestDto,
  ResetPasswordRequestDto
} from "./dto.js";
import { AuthService, HttpError } from "../../services/auth.service.js";

async function execute(
  response: ServerResponse,
  action: () => Promise<unknown>
): Promise<void> {
  try {
    const payload = await action();
    sendJson(response, 200, payload);
  } catch (error) {
    if (error instanceof HttpError) {
      sendJson(response, error.statusCode, error.payload ?? { message: error.message });
      return;
    }

    sendJson(response, 500, { message: "Internal server error" });
  }
}

export async function handleAuthRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  authService: AuthService
): Promise<boolean> {
  if (request.method === "POST" && request.url === "/auth/login") {
    const body = await readJsonBody<LoginRequestDto>(request);
    await execute(response, () => authService.login(body));
    return true;
  }

  if (request.method === "POST" && request.url === "/auth/manual-register") {
    const body = await readJsonBody<ManualRegisterRequestDto>(request);
    await execute(response, () => authService.manualRegister(body));
    return true;
  }

  if (request.method === "POST" && request.url === "/auth/forgot-password") {
    const body = await readJsonBody<OtpRequestDto>(request);
    await execute(response, () => authService.sendOtp(body));
    return true;
  }

  if (request.method === "POST" && request.url === "/auth/verify-otp") {
    const body = await readJsonBody<OtpVerifyRequestDto>(request);
    await execute(response, () => authService.verifyOtp(body));
    return true;
  }

  if (request.method === "POST" && request.url === "/auth/reset-password") {
    const body = await readJsonBody<ResetPasswordRequestDto>(request);
    await execute(response, () => authService.resetPassword(body));
    return true;
  }

  if (request.method === "POST" && request.url === "/auth/refresh") {
    const body = await readJsonBody<RefreshRequestDto>(request);
    await execute(response, () => authService.refresh(body));
    return true;
  }

  if (request.method === "POST" && request.url === "/auth/logout") {
    const body = await readJsonBody<RefreshRequestDto>(request);
    await execute(response, () => authService.logout(body));
    return true;
  }

  if (request.method === "POST" && request.url === "/auth/forgot-ticket") {
    const body = await readJsonBody<ForgotTicketRequestDto>(request);
    await execute(response, () => authService.forgotTicket(body));
    return true;
  }

  return false;
}

