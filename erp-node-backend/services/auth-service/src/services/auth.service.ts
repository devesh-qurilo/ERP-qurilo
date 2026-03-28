import type { RefreshTokenRepository } from "../repositories/refresh-token.repository.js";
import type { UserRepository } from "../repositories/user.repository.js";
import type {
  ForgotTicketRequestDto,
  LoginRequestDto,
  LoginResponseDto,
  ManualRegisterRequestDto,
  OtpRequestDto,
  OtpVerifyRequestDto,
  RefreshRequestDto,
  ResetPasswordRequestDto
} from "../modules/auth/dto.js";
import type { MailService } from "./mail.service.js";
import type { PasswordService } from "./password.service.js";
import type { UserRecord } from "../types/models.js";

import { TokenService } from "./token.service.js";

export class HttpError extends Error {
  constructor(
    public readonly statusCode: number,
    message: string,
    public readonly payload?: Record<string, unknown>
  ) {
    super(message);
  }
}

export class AuthService {
  constructor(
    private readonly users: UserRepository,
    private readonly refreshTokens: RefreshTokenRepository,
    private readonly passwordService: PasswordService,
    private readonly tokenService: TokenService,
    private readonly mailService: MailService
  ) {}

  async login(request: LoginRequestDto): Promise<LoginResponseDto> {
    const employeeId = normalizeEmployeeId(request.employeeId);
    const user = await this.users.findByEmployeeId(employeeId);

    if (!user) {
      throw new HttpError(401, "Invalid credentials");
    }

    const passwordMatches = await this.passwordService.compare(request.password, user.password);

    if (!passwordMatches) {
      throw new HttpError(401, "Invalid credentials");
    }

    if (this.passwordService.needsUpgrade(user.password)) {
      await this.users.save({
        ...user,
        password: await this.passwordService.hash(request.password)
      });
    }

    const tokens = this.tokenService.generateTokenPair(user.employeeId, user.role);
    const expiry = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);

    await this.refreshTokens.save({
      id: 0,
      token: tokens.refreshToken,
      employeeId: user.employeeId,
      expiry,
      active: true
    });

    return {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      role: user.role,
      employeeId: user.employeeId
    };
  }

  async manualRegister(request: ManualRegisterRequestDto): Promise<Record<string, string>> {
    const employeeId = normalizeEmployeeId(request.employeeId);
    const existingUser = await this.users.findByEmployeeId(employeeId);

    if (existingUser) {
      throw new HttpError(409, "User already exists", { status: "error", message: "User already exists" });
    }

    await this.users.save({
      id: 0,
      employeeId,
      password: await this.passwordService.hash(request.password),
      role: request.role.trim().toUpperCase(),
      email: request.email?.trim().toLowerCase() ?? null,
      active: true,
      otp: null,
      otpExpiry: null
    });

    return { status: "success", message: "User created" };
  }

  async sendOtp(request: OtpRequestDto): Promise<Record<string, string>> {
    const user = await this.users.findByEmployeeId(normalizeEmployeeId(request.employeeId));

    if (!user || user.email?.toLowerCase() !== request.email.toLowerCase()) {
      throw new HttpError(404, "Invalid employeeId or email", {
        status: "error",
        message: "Invalid employeeId or email"
      });
    }

    const otp = String(Math.floor(100000 + Math.random() * 900000));

    await this.users.save({
      ...user,
      otp,
      otpExpiry: new Date(Date.now() + 10 * 60 * 1000)
    });

    await this.mailService.sendOtpMail(user.email, otp);

    return { status: "success", message: "OTP sent to email" };
  }

  async verifyOtp(request: OtpVerifyRequestDto): Promise<Record<string, string>> {
    const user = await this.users.findByEmployeeId(normalizeEmployeeId(request.employeeId));

    if (!user || user.otp !== request.otp || !user.otpExpiry || user.otpExpiry.getTime() <= Date.now()) {
      throw new HttpError(401, "Invalid or expired OTP", {
        status: "error",
        message: "Invalid or expired OTP"
      });
    }

    return { status: "success", message: "OTP verified" };
  }

  async resetPassword(request: ResetPasswordRequestDto): Promise<Record<string, string>> {
    const user = await this.users.findByEmployeeId(normalizeEmployeeId(request.employeeId));

    if (!user || !user.otp || !user.otpExpiry || user.otpExpiry.getTime() <= Date.now()) {
      throw new HttpError(401, "OTP not verified or expired", {
        status: "error",
        message: "OTP not verified or expired"
      });
    }

    await this.users.save({
      ...user,
      password: await this.passwordService.hash(request.newPassword),
      otp: null,
      otpExpiry: null
    });

    return { status: "success", message: "Password reset successfully" };
  }

  async refresh(request: RefreshRequestDto): Promise<LoginResponseDto> {
    if (!request.refreshToken?.trim()) {
      throw new HttpError(400, "Refresh token is required");
    }

    const tokenRecord = await this.refreshTokens.findActiveByToken(request.refreshToken);

    if (!tokenRecord || tokenRecord.expiry.getTime() <= Date.now()) {
      throw new HttpError(401, "Invalid or expired refresh token");
    }

    const claims = this.tokenService.validateRefreshToken(request.refreshToken);
    const user = await this.users.findByEmployeeId(claims.sub);

    if (!user) {
      throw new HttpError(404, "User not found");
    }

    const newAccessToken = this.tokenService.generateTokenPair(user.employeeId, user.role).accessToken;

    return {
      accessToken: newAccessToken,
      refreshToken: request.refreshToken,
      role: user.role,
      employeeId: user.employeeId
    };
  }

  async logout(request: RefreshRequestDto): Promise<Record<string, string>> {
    if (!request.refreshToken?.trim()) {
      throw new HttpError(400, "Refresh token is required");
    }

    await this.refreshTokens.invalidate(request.refreshToken);
    return { message: "Logout successful" };
  }

  async forgotTicket(request: ForgotTicketRequestDto): Promise<Record<string, string>> {
    if (!request.employeeId || !request.name) {
      throw new HttpError(400, "name & employeeId required", {
        status: "error",
        message: "name & employeeId required"
      });
    }

    const subject = `ERP Password Reset Ticket: ${request.employeeId}`;
    const lines = [
      "Password reset ticket received:",
      "",
      `Name: ${request.name}`,
      request.email ? `Email: ${request.email}` : null,
      `EmployeeId: ${request.employeeId}`,
      `Designation: ${request.designation ?? "-"}`,
      `Department: ${request.department ?? "-"}`,
      "",
      "Please process this ticket and update the user's password via internal APIs."
    ].filter(Boolean);

    await this.mailService.sendAdminNotification(subject, lines.join("\n"));

    return { status: "success", message: "Ticket submitted to admin" };
  }

  async registerInternal(body: Record<string, string | undefined>): Promise<Record<string, string>> {
    const employeeId = body.employeeId ? normalizeEmployeeId(body.employeeId) : undefined;
    const password = body.password;
    const role = body.role?.trim().toUpperCase() ?? "ROLE_EMPLOYEE";
    const email = body.email?.trim().toLowerCase();

    if (!employeeId || !password) {
      throw new HttpError(400, "employeeId & password required", {
        status: "error",
        message: "employeeId & password required"
      });
    }

    const existingUser = await this.users.findByEmployeeId(employeeId);

    if (existingUser) {
      throw new HttpError(409, "User already exists", {
        status: "error",
        message: "User already exists"
      });
    }

    await this.users.save({
      id: 0,
      employeeId,
      password: await this.passwordService.hash(password),
      role,
      email: email ?? null,
      active: true,
      otp: null,
      otpExpiry: null
    });

    return { status: "success", message: "User registered" };
  }

  async updateRole(body: Record<string, string | undefined>): Promise<Record<string, string>> {
    const user = await this.getExistingUser(body.employeeId, "employeeId & role required", body.role);

    await this.users.save({
      ...user,
      role: body.role!.trim().toUpperCase()
    });

    return { status: "success", message: "Role updated" };
  }

  async updatePassword(body: Record<string, string | undefined>): Promise<Record<string, string>> {
    const user = await this.getExistingUser(body.employeeId, "employeeId & password required", body.password);

    await this.users.save({
      ...user,
      password: await this.passwordService.hash(body.password!)
    });

    return { status: "success", message: "Password updated" };
  }

  async updateEmail(body: Record<string, string | undefined>): Promise<Record<string, string>> {
    const user = await this.getExistingUser(body.employeeId, "employeeId & email required", body.email);

    await this.users.save({
      ...user,
      email: body.email!.trim().toLowerCase()
    });

    return { status: "success", message: "Email updated" };
  }

  async deleteUser(employeeId: string): Promise<Record<string, string>> {
    const deleted = await this.users.deleteByEmployeeId(normalizeEmployeeId(employeeId));

    if (!deleted) {
      throw new HttpError(404, "User not found", {
        status: "error",
        message: "User not found"
      });
    }

    return { status: "success", message: "User deleted" };
  }

  async seedDefaultAdmin(): Promise<void> {
    const existingUser = await this.users.findByEmployeeId("ADMIN-001");

    if (existingUser) {
      return;
    }

    await this.users.save({
      id: 0,
      employeeId: "ADMIN-001",
      password: await this.passwordService.hash("admin123"),
      role: "ROLE_ADMIN",
      email: "admin@example.com",
      active: true,
      otp: null,
      otpExpiry: null
    });
  }

  private async getExistingUser(
    employeeId: string | undefined,
    badRequestMessage: string,
    requiredValue?: string
  ): Promise<UserRecord> {
    if (!employeeId || !requiredValue) {
      throw new HttpError(400, badRequestMessage, {
        status: "error",
        message: badRequestMessage
      });
    }

    const user = await this.users.findByEmployeeId(normalizeEmployeeId(employeeId));
    if (!user) {
      throw new HttpError(404, "User not found", {
        status: "error",
        message: "User not found"
      });
    }

    return user;
  }
}

function normalizeEmployeeId(employeeId: string): string {
  return employeeId.trim().toUpperCase();
}
