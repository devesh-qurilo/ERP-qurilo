import type { Notification, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import type { SendNotificationDto, SendNotificationManyDto } from "../modules/notification/dto.js";
import { PushService } from "./push.service.js";

type NotificationWithRelations = Notification & {
  sender: { employeeId: string } | null;
  receiver: { employeeId: string };
};

export class NotificationService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly pushService: PushService
  ) {}

  async getById(id: number, employeeId?: string): Promise<Record<string, unknown>> {
    const notification = await this.prisma.notification.findUnique({
      where: { id: BigInt(id) },
      include: {
        sender: { select: { employeeId: true } },
        receiver: { select: { employeeId: true } }
      }
    });

    if (!notification) {
      throw new HttpError(404, "Notification not found");
    }

    if (employeeId && notification.receiver.employeeId !== employeeId) {
      throw new HttpError(403, "Not allowed");
    }

    return mapNotification(notification);
  }

  async getMyNotifications(employeeId: string): Promise<Record<string, unknown>[]> {
    const normalizedEmployeeId = employeeId.trim().toUpperCase();
    await this.ensureEmployee(normalizedEmployeeId);

    const notifications = await this.prisma.notification.findMany({
      where: { receiverEmployeeId: normalizedEmployeeId },
      include: {
        sender: { select: { employeeId: true } },
        receiver: { select: { employeeId: true } }
      },
      orderBy: { createdAt: "desc" }
    });

    return notifications.map(mapNotification);
  }

  async markRead(id: number, employeeId: string): Promise<void> {
    const notification = await this.ensureOwnedNotification(id, employeeId);
    await this.prisma.notification.update({
      where: { id: notification.id },
      data: {
        readFlag: true,
        readAt: new Date()
      }
    });
  }

  async markUnread(id: number, employeeId: string): Promise<void> {
    const notification = await this.ensureOwnedNotification(id, employeeId);
    await this.prisma.notification.update({
      where: { id: notification.id },
      data: {
        readFlag: false,
        readAt: null
      }
    });
  }

  async clearAll(employeeId: string): Promise<void> {
    await this.ensureEmployee(employeeId.trim().toUpperCase());
    await this.prisma.notification.deleteMany({
      where: { receiverEmployeeId: employeeId.trim().toUpperCase() }
    });
  }

  async sendNotification(senderEmployeeId: string | null, dto: SendNotificationDto): Promise<void> {
    const receiverEmployeeId = dto.receiverEmployeeId?.trim().toUpperCase();
    const title = dto.title?.trim();
    const message = dto.message?.trim();

    if (!receiverEmployeeId || !title || !message) {
      throw new HttpError(400, "receiverEmployeeId, title and message are required");
    }

    const normalizedSenderEmployeeId = senderEmployeeId?.trim().toUpperCase() || null;
    const senderExists = normalizedSenderEmployeeId
      ? await this.prisma.employee.findUnique({
          where: { employeeId: normalizedSenderEmployeeId },
          select: { employeeId: true }
        })
      : null;

    await this.ensureEmployee(receiverEmployeeId);

    const notification = await this.prisma.notification.create({
      data: {
        senderEmployeeId: senderExists?.employeeId ?? null,
        receiverEmployeeId,
        title,
        message,
        type: dto.type?.trim() || null,
        readFlag: false
      }
    });

    await this.pushService.sendPushToUser(receiverEmployeeId, notification.title, notification.message, {
      module: "employee",
      notificationId: String(notification.id)
    });
  }

  async sendNotificationMany(senderEmployeeId: string | null, dto: SendNotificationManyDto): Promise<void> {
    const receiverEmployeeIds = dto.receiverEmployeeIds?.map((employeeId) => employeeId.trim().toUpperCase()).filter(Boolean) ?? [];
    const title = dto.title?.trim();
    const message = dto.message?.trim();

    if (!receiverEmployeeIds.length || !title || !message) {
      throw new HttpError(400, "receiverEmployeeIds, title and message are required");
    }

    const normalizedSenderEmployeeId = senderEmployeeId?.trim().toUpperCase() || null;
    const senderExists = normalizedSenderEmployeeId
      ? await this.prisma.employee.findUnique({
          where: { employeeId: normalizedSenderEmployeeId },
          select: { employeeId: true }
        })
      : null;

    for (const receiverEmployeeId of receiverEmployeeIds) {
      await this.ensureEmployee(receiverEmployeeId);
      const notification = await this.prisma.notification.create({
        data: {
          senderEmployeeId: senderExists?.employeeId ?? null,
          receiverEmployeeId,
          title,
          message,
          type: dto.type?.trim() || null,
          readFlag: false
        }
      });

      await this.pushService.sendPushToUser(receiverEmployeeId, notification.title, notification.message, {
        module: "employee",
        notificationId: String(notification.id)
      });
    }
  }

  async deleteById(id: number, employeeId: string, isAdmin = false): Promise<void> {
    const notification = await this.prisma.notification.findUnique({
      where: { id: BigInt(id) },
      select: { id: true, receiverEmployeeId: true }
    });

    if (!notification) {
      throw new HttpError(404, "Notification not found");
    }

    const normalizedEmployeeId = employeeId.trim().toUpperCase();
    if (!isAdmin && notification.receiverEmployeeId !== normalizedEmployeeId) {
      throw new HttpError(403, "Not allowed");
    }

    await this.prisma.notification.delete({
      where: { id: BigInt(id) }
    });
  }

  private async ensureOwnedNotification(id: number, employeeId: string): Promise<{ id: bigint; receiverEmployeeId: string }> {
    const notification = await this.prisma.notification.findUnique({
      where: { id: BigInt(id) },
      select: { id: true, receiverEmployeeId: true }
    });

    if (!notification) {
      throw new HttpError(404, "Notification not found");
    }

    if (notification.receiverEmployeeId !== employeeId.trim().toUpperCase()) {
      throw new HttpError(403, "Not allowed");
    }

    return notification;
  }

  private async ensureEmployee(employeeId: string): Promise<void> {
    const employee = await this.prisma.employee.findUnique({
      where: { employeeId },
      select: { employeeId: true }
    });

    if (!employee) {
      throw new HttpError(404, `Employee not found: ${employeeId}`);
    }
  }
}

function mapNotification(notification: NotificationWithRelations): Record<string, unknown> {
  return {
    id: Number(notification.id),
    senderEmployeeId: notification.sender?.employeeId ?? null,
    receiverEmployeeId: notification.receiver.employeeId,
    title: notification.title,
    message: notification.message,
    type: notification.type,
    readFlag: notification.readFlag,
    createdAt: notification.createdAt,
    readAt: notification.readAt
  };
}
