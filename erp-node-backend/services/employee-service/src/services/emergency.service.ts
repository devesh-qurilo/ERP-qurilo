import type { EmergencyContact, Employee, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import type { EmergencyContactRequestDto, UpdateEmergencyContactDto } from "../modules/emergency/dto.js";

type EmergencyContactWithEmployee = EmergencyContact & {
  employee: Employee;
};

export class EmergencyService {
  constructor(private readonly prisma: PrismaClient) {}

  async createEmergencyContact(employeeId: string, dto: EmergencyContactRequestDto): Promise<Record<string, unknown>> {
    const employee = await this.ensureEmployee(employeeId);
    validateRequired(dto);

    const saved = await this.prisma.emergencyContact.create({
      data: {
        name: dto.name!.trim(),
        email: dto.email!.trim().toLowerCase(),
        mobile: dto.mobile!.trim(),
        relationship: dto.relationship!.trim(),
        address: dto.address!.trim(),
        employeeId: employee.employeeId
      },
      include: { employee: true }
    });

    return mapEmergencyContact(saved);
  }

  async getAllByEmployeeId(employeeId: string): Promise<Record<string, unknown>[]> {
    await this.ensureEmployee(employeeId);

    const contacts = await this.prisma.emergencyContact.findMany({
      where: { employeeId: normalizeEmployeeId(employeeId) },
      include: { employee: true },
      orderBy: { id: "asc" }
    });

    return contacts.map(mapEmergencyContact);
  }

  async getByEmployeeIdAndContactId(employeeId: string, id: number): Promise<Record<string, unknown>> {
    const contact = await this.findContactOrThrow(employeeId, id);
    return mapEmergencyContact(contact);
  }

  async updateEmergencyContact(
    employeeId: string,
    id: number,
    dto: UpdateEmergencyContactDto
  ): Promise<Record<string, unknown>> {
    const contact = await this.findContactOrThrow(employeeId, id);

    const updated = await this.prisma.emergencyContact.update({
      where: { id: BigInt(id) },
      data: {
        name: dto.name !== undefined ? dto.name.trim() : contact.name,
        email: dto.email !== undefined ? dto.email.trim().toLowerCase() : contact.email,
        mobile: dto.mobile !== undefined ? dto.mobile.trim() : contact.mobile,
        relationship: dto.relationship !== undefined ? dto.relationship.trim() : contact.relationship,
        address: dto.address !== undefined ? dto.address.trim() : contact.address
      },
      include: { employee: true }
    });

    return mapEmergencyContact(updated);
  }

  async deleteByEmployeeIdAndContactId(employeeId: string, id: number): Promise<void> {
    await this.findContactOrThrow(employeeId, id);
    await this.prisma.emergencyContact.delete({
      where: { id: BigInt(id) }
    });
  }

  private async ensureEmployee(employeeId: string): Promise<Employee> {
    const employee = await this.prisma.employee.findUnique({
      where: { employeeId: normalizeEmployeeId(employeeId) }
    });

    if (!employee) {
      throw new HttpError(404, `Employee not found with id: ${employeeId}`);
    }

    return employee;
  }

  private async findContactOrThrow(employeeId: string, id: number): Promise<EmergencyContactWithEmployee> {
    await this.ensureEmployee(employeeId);

    const contact = await this.prisma.emergencyContact.findFirst({
      where: {
        id: BigInt(id),
        employeeId: normalizeEmployeeId(employeeId)
      },
      include: { employee: true }
    });

    if (!contact) {
      throw new HttpError(404, `Emergency contact not found with id: ${id}`);
    }

    return contact;
  }
}

function mapEmergencyContact(contact: EmergencyContactWithEmployee): Record<string, unknown> {
  return {
    id: Number(contact.id),
    name: contact.name,
    email: contact.email,
    mobile: contact.mobile,
    relationship: contact.relationship,
    address: contact.address,
    employeeId: contact.employee.employeeId
  };
}

function validateRequired(dto: EmergencyContactRequestDto): void {
  if (!dto.name?.trim() || !dto.email?.trim() || !dto.mobile?.trim() || !dto.relationship?.trim() || !dto.address?.trim()) {
    throw new HttpError(400, "name, email, mobile, relationship and address are required");
  }
}

function normalizeEmployeeId(employeeId: string): string {
  return employeeId.trim().toUpperCase();
}
