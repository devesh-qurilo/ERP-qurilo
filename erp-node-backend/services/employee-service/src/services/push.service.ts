import type { PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";

export class PushService {
  constructor(private readonly prisma: PrismaClient) {}

  async registerToken(
    employeeId: string,
    provider?: string,
    token?: string,
    deviceInfo?: string | null
  ): Promise<void> {
    if (!provider?.trim() || !token?.trim()) {
      throw new HttpError(400, "provider & token required");
    }

    const normalizedEmployeeId = employeeId.trim().toUpperCase();
    const normalizedProvider = provider.trim().toUpperCase();
    const normalizedToken = token.trim();

    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: normalizedEmployeeId },
      select: { employeeId: true }
    });

    if (!employee) {
      throw new HttpError(404, "Employee not found");
    }

    const existing = await this.prisma.pushToken.findUnique({
      where: { token: normalizedToken }
    });

    if (existing) {
      await this.prisma.pushToken.update({
        where: { token: normalizedToken },
        data: {
          employeeId: normalizedEmployeeId,
          provider: normalizedProvider,
          deviceInfo: deviceInfo?.trim() || null,
          lastSeen: new Date()
        }
      });
      return;
    }

    await this.prisma.pushToken.create({
      data: {
        employeeId: normalizedEmployeeId,
        provider: normalizedProvider,
        token: normalizedToken,
        deviceInfo: deviceInfo?.trim() || null,
        lastSeen: new Date()
      }
    });
  }

  async unregisterToken(token?: string): Promise<void> {
    if (!token?.trim()) {
      throw new HttpError(400, "token required");
    }

    await this.prisma.pushToken.deleteMany({
      where: { token: token.trim() }
    });
  }

  async sendPushToUser(employeeId: string, title: string, body: string, data?: Record<string, string>): Promise<void> {
    await this.sendPushToUsers([employeeId], title, body, data);
  }

  async sendPushToUsers(employeeIds: string[], title: string, body: string, data?: Record<string, string>): Promise<void> {
    const normalizedEmployeeIds = employeeIds.map((employeeId) => employeeId.trim().toUpperCase()).filter(Boolean);
    if (!normalizedEmployeeIds.length) {
      return;
    }

    const tokens = await this.prisma.pushToken.findMany({
      where: { employeeId: { in: normalizedEmployeeIds } }
    });

    if (!tokens.length) {
      return;
    }

    // Best-effort stub for now. We persist tokens and treat delivery as non-blocking
    void title;
    void body;
    void data;
  }
}
