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

