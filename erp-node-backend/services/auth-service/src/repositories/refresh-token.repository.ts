import type { PrismaClient, RefreshToken } from "@prisma/client";
import type { RefreshTokenRecord } from "../types/models.js";

export interface RefreshTokenRepository {
  findActiveByToken(token: string): Promise<RefreshTokenRecord | null>;
  save(token: RefreshTokenRecord): Promise<RefreshTokenRecord>;
  invalidate(token: string): Promise<void>;
}

let nextRefreshTokenId = 1;

export class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
  private readonly tokens = new Map<string, RefreshTokenRecord>();

  async findActiveByToken(token: string): Promise<RefreshTokenRecord | null> {
    const record = this.tokens.get(token) ?? null;

    if (!record || !record.active) {
      return null;
    }

    return record;
  }

  async save(token: RefreshTokenRecord): Promise<RefreshTokenRecord> {
    const normalizedRecord: RefreshTokenRecord = {
      ...token,
      id: token.id || nextRefreshTokenId++
    };

    this.tokens.set(normalizedRecord.token, normalizedRecord);
    return normalizedRecord;
  }

  async invalidate(token: string): Promise<void> {
    const existing = this.tokens.get(token);

    if (!existing) {
      return;
    }

    this.tokens.set(token, { ...existing, active: false });
  }
}

export class PrismaRefreshTokenRepository implements RefreshTokenRepository {
  constructor(private readonly prisma: PrismaClient) {}

  async findActiveByToken(token: string): Promise<RefreshTokenRecord | null> {
    const record = await this.prisma.refreshToken.findFirst({
      where: {
        token,
        active: true
      }
    });

    return record ? mapRefreshTokenRecord(record) : null;
  }

  async save(token: RefreshTokenRecord): Promise<RefreshTokenRecord> {
    const savedToken = await this.prisma.refreshToken.upsert({
      where: {
        token: token.token
      },
      create: {
        token: token.token,
        employeeId: token.employeeId,
        expiry: token.expiry,
        active: token.active
      },
      update: {
        employeeId: token.employeeId,
        expiry: token.expiry,
        active: token.active
      }
    });

    return mapRefreshTokenRecord(savedToken);
  }

  async invalidate(token: string): Promise<void> {
    await this.prisma.refreshToken.updateMany({
      where: {
        token
      },
      data: {
        active: false
      }
    });
  }
}

function mapRefreshTokenRecord(token: RefreshToken): RefreshTokenRecord {
  return {
    id: Number(token.id),
    token: token.token,
    employeeId: token.employeeId,
    expiry: token.expiry,
    active: token.active
  };
}
