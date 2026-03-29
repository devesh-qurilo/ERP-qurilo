import type { Department, Designation, Employee, Promotion, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import { NotificationService } from "./notification.service.js";

type PromotionWithRelations = Promotion & {
  employee: Employee;
  oldDepartment: Department;
  oldDesignation: Designation;
  newDepartment: Department;
  newDesignation: Designation;
};

const promotionInclude = {
  employee: true,
  oldDepartment: true,
  oldDesignation: true,
  newDepartment: true,
  newDesignation: true
} as const;

export class PromotionService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly notificationService: NotificationService
  ) {}

  async createPromotion(
    employeeId: string,
    request: {
      newDepartmentId?: number;
      newDesignationId?: number;
      sendNotification?: boolean;
      isPromotion?: boolean;
      remarks?: string | null;
    },
    actorEmployeeId: string
  ): Promise<Record<string, unknown>> {
    const employee = await this.requireEmployee(employeeId);
    const sendNotification = request.sendNotification;
    const isPromotion = request.isPromotion;

    if (!employee.departmentId || !employee.designationId) {
      throw new HttpError(400, "Employee does not have current department or designation assigned");
    }

    if (!request.newDepartmentId || !request.newDesignationId || sendNotification === undefined || isPromotion === undefined) {
      throw new HttpError(400, "newDepartmentId, newDesignationId, sendNotification and isPromotion are required");
    }

    const [newDepartment, newDesignation] = await Promise.all([
      this.requireDepartment(BigInt(request.newDepartmentId)),
      this.requireDesignation(BigInt(request.newDesignationId))
    ]);

    const promotionId = await this.prisma.$transaction(async (tx) => {
      const saved = await tx.promotion.create({
        data: {
          employeeId: employee.employeeId,
          oldDepartmentId: employee.departmentId!,
          oldDesignationId: employee.designationId!,
          newDepartmentId: newDepartment.id,
          newDesignationId: newDesignation.id,
          isPromotion,
          sendNotification,
          remarks: request.remarks?.trim() || null
        }
      });

      await tx.employee.update({
        where: { employeeId: employee.employeeId },
        data: {
          departmentId: newDepartment.id,
          designationId: newDesignation.id
        }
      });

      return saved.id;
    });

    const promotion = await this.getPromotionRecord(Number(promotionId));

    if (sendNotification) {
      await this.notificationService.sendNotification(actorEmployeeId, {
        receiverEmployeeId: employee.employeeId,
        title: isPromotion ? "Promotion Notification" : "Demotion Notification",
        message: isPromotion
          ? `Congratulations! You have been promoted from ${promotion.oldDesignation.designationName} (${promotion.oldDepartment.departmentName}) to ${promotion.newDesignation.designationName} (${promotion.newDepartment.departmentName}).`
          : `Your designation has been changed from ${promotion.oldDesignation.designationName} (${promotion.oldDepartment.departmentName}) to ${promotion.newDesignation.designationName} (${promotion.newDepartment.departmentName}).`,
        type: "PROMOTION"
      });
    }

    return mapPromotion(promotion);
  }

  async updatePromotion(
    id: number,
    request: {
      newDepartmentId?: number;
      newDesignationId?: number;
      sendNotification?: boolean;
      isPromotion?: boolean;
      remarks?: string | null;
    },
    _actorEmployeeId: string
  ): Promise<Record<string, unknown>> {
    const existing = await this.getPromotionRecord(id);
    const sendNotification = request.sendNotification;
    const isPromotion = request.isPromotion;

    if (!request.newDepartmentId || !request.newDesignationId || sendNotification === undefined || isPromotion === undefined) {
      throw new HttpError(400, "newDepartmentId, newDesignationId, sendNotification and isPromotion are required");
    }

    const [newDepartment, newDesignation] = await Promise.all([
      this.requireDepartment(BigInt(request.newDepartmentId)),
      this.requireDesignation(BigInt(request.newDesignationId))
    ]);

    const updated = await this.prisma.$transaction(async (tx) => {
      const saved = await tx.promotion.update({
        where: { id: BigInt(id) },
        data: {
          newDepartmentId: newDepartment.id,
          newDesignationId: newDesignation.id,
          sendNotification,
          isPromotion,
          remarks: request.remarks?.trim() || null
        },
        include: promotionInclude
      });

      await tx.employee.update({
        where: { employeeId: existing.employeeId },
        data: {
          departmentId: newDepartment.id,
          designationId: newDesignation.id
        }
      });

      return saved;
    });

    return mapPromotion(updated);
  }

  async getAllPromotions(): Promise<Record<string, unknown>[]> {
    const promotions = await this.prisma.promotion.findMany({
      include: promotionInclude,
      orderBy: { createdAt: "desc" }
    });

    return promotions.map(mapPromotion);
  }

  async getPromotionsByEmployee(employeeId: string): Promise<Record<string, unknown>[]> {
    const promotions = await this.prisma.promotion.findMany({
      where: { employeeId: employeeId.trim().toUpperCase() },
      include: promotionInclude,
      orderBy: { createdAt: "desc" }
    });

    return promotions.map(mapPromotion);
  }

  async getPromotionById(id: number): Promise<Record<string, unknown>> {
    return mapPromotion(await this.getPromotionRecord(id));
  }

  async deletePromotion(id: number): Promise<void> {
    await this.getPromotionRecord(id);
    await this.prisma.promotion.delete({
      where: { id: BigInt(id) }
    });
  }

  private async requireEmployee(employeeId: string): Promise<Employee> {
    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: employeeId.trim().toUpperCase() }
    });

    if (!employee) {
      throw new HttpError(404, `Employee not found with ID: ${employeeId}`);
    }

    return employee;
  }

  private async requireDepartment(id: bigint): Promise<Department> {
    const department = await this.prisma.department.findUnique({
      where: { id }
    });

    if (!department) {
      throw new HttpError(404, `Department not found with ID: ${id}`);
    }

    return department;
  }

  private async requireDesignation(id: bigint): Promise<Designation> {
    const designation = await this.prisma.designation.findUnique({
      where: { id }
    });

    if (!designation) {
      throw new HttpError(404, `Designation not found with ID: ${id}`);
    }

    return designation;
  }

  private async getPromotionRecord(id: number): Promise<PromotionWithRelations> {
    const promotion = await this.prisma.promotion.findUnique({
      where: { id: BigInt(id) },
      include: promotionInclude
    });

    if (!promotion) {
      throw new HttpError(404, `Promotion record not found with ID: ${id}`);
    }

    return promotion;
  }
}

function mapPromotion(promotion: PromotionWithRelations): Record<string, unknown> {
  return {
    id: Number(promotion.id),
    employeeId: promotion.employee.employeeId,
    employeeName: promotion.employee.name,
    oldDepartmentId: Number(promotion.oldDepartmentId),
    oldDepartmentName: promotion.oldDepartment.departmentName,
    oldDesignationId: Number(promotion.oldDesignationId),
    oldDesignationName: promotion.oldDesignation.designationName,
    newDepartmentId: Number(promotion.newDepartmentId),
    newDepartmentName: promotion.newDepartment.departmentName,
    newDesignationId: Number(promotion.newDesignationId),
    newDesignationName: promotion.newDesignation.designationName,
    isPromotion: promotion.isPromotion,
    sendNotification: promotion.sendNotification,
    createdAt: promotion.createdAt,
    remarks: promotion.remarks
  };
}
