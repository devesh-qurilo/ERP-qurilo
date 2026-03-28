import type { PrismaClient, User } from "@prisma/client";
import type { UserRecord } from "../types/models.js";

export interface UserRepository {
  findByEmployeeId(employeeId: string): Promise<UserRecord | null>;
  save(user: UserRecord): Promise<UserRecord>;
  deleteByEmployeeId(employeeId: string): Promise<boolean>;
}

let nextUserId = 1;

export class InMemoryUserRepository implements UserRepository {
  private readonly users = new Map<string, UserRecord>();

  async findByEmployeeId(employeeId: string): Promise<UserRecord | null> {
    return this.users.get(employeeId.trim()) ?? null;
  }

  async save(user: UserRecord): Promise<UserRecord> {
    const normalizedRecord: UserRecord = {
      ...user,
      id: user.id || nextUserId++,
      employeeId: user.employeeId.trim()
    };

    this.users.set(normalizedRecord.employeeId, normalizedRecord);
    return normalizedRecord;
  }

  async deleteByEmployeeId(employeeId: string): Promise<boolean> {
    return this.users.delete(employeeId.trim());
  }
}

export class PrismaUserRepository implements UserRepository {
  constructor(private readonly prisma: PrismaClient) {}

  async findByEmployeeId(employeeId: string): Promise<UserRecord | null> {
    const user = await this.prisma.user.findUnique({
      where: {
        employeeId: employeeId.trim()
      }
    });

    return user ? mapUserRecord(user) : null;
  }

  async save(user: UserRecord): Promise<UserRecord> {
    const savedUser = await this.prisma.user.upsert({
      where: {
        employeeId: user.employeeId.trim()
      },
      create: {
        employeeId: user.employeeId.trim(),
        password: user.password,
        role: user.role,
        email: user.email,
        active: user.active,
        otp: user.otp,
        otpExpiry: user.otpExpiry
      },
      update: {
        password: user.password,
        role: user.role,
        email: user.email,
        active: user.active,
        otp: user.otp,
        otpExpiry: user.otpExpiry
      }
    });

    return mapUserRecord(savedUser);
  }

  async deleteByEmployeeId(employeeId: string): Promise<boolean> {
    const normalizedEmployeeId = employeeId.trim();
    const deleted = await this.prisma.$transaction(async (tx) => {
      await tx.refreshToken.deleteMany({
        where: {
          employeeId: normalizedEmployeeId
        }
      });

      const result = await tx.user.deleteMany({
        where: {
          employeeId: normalizedEmployeeId
        }
      });

      return result.count;
    });

    return deleted > 0;
  }
}

function mapUserRecord(user: User): UserRecord {
  return {
    id: Number(user.id),
    employeeId: user.employeeId,
    password: user.password,
    role: user.role,
    email: user.email,
    active: user.active,
    otp: user.otp,
    otpExpiry: user.otpExpiry
  };
}
