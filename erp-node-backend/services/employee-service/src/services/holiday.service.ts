import type { Holiday, PrismaClient } from "@prisma/client";

import { HttpError } from "../common/errors.js";
import type { BulkHolidayRequestDto, DefaultHolidaysRequestDto, HolidayRequestDto } from "../modules/holiday/dto.js";

export class HolidayService {
  constructor(private readonly prisma: PrismaClient) {}

  async createHoliday(requestDto: HolidayRequestDto): Promise<Record<string, unknown>> {
    const date = requireHolidayDate(requestDto.date);
    const occasion = requestDto.occasion?.trim();

    if (!occasion) {
      throw new HttpError(400, "Occasion is required");
    }

    await ensureUniqueHolidayDate(this.prisma, date);

    const holiday = await this.prisma.holiday.create({
      data: {
        date,
        day: getDayName(date),
        occasion,
        isDefaultWeekly: false,
        isActive: true
      }
    });

    return mapHoliday(holiday);
  }

  async createBulkHolidays(requestDto: BulkHolidayRequestDto): Promise<Record<string, unknown>[]> {
    const holidays = requestDto.holidays ?? [];

    const created: Holiday[] = [];
    for (const holidayDto of holidays) {
      const date = requireHolidayDate(holidayDto.date);
      const occasion = holidayDto.occasion?.trim();

      if (!occasion) {
        throw new HttpError(400, "Occasion is required");
      }

      const existing = await this.prisma.holiday.findUnique({
        where: { date }
      });

      if (existing) {
        continue;
      }

      created.push(
        await this.prisma.holiday.create({
          data: {
            date,
            day: getDayName(date),
            occasion,
            isDefaultWeekly: false,
            isActive: true
          }
        })
      );
    }

    return created.map(mapHoliday);
  }

  async setDefaultWeeklyHolidays(requestDto: DefaultHolidaysRequestDto): Promise<Record<string, unknown>[]> {
    const year = requestDto.year ?? new Date().getUTCFullYear();
    const month = requestDto.month ?? new Date().getUTCMonth() + 1;
    const occasion = requestDto.occasion?.trim() || "Weekly Holiday";
    const weekDays = (requestDto.weekDays ?? []).map((day) => day.trim().toUpperCase()).filter(Boolean);

    if (!weekDays.length) {
      throw new HttpError(400, "weekDays is required");
    }

    if (month < 1 || month > 12) {
      throw new HttpError(400, "month must be between 1 and 12");
    }

    const startDate = new Date(Date.UTC(year, month - 1, 1));
    const endDate = new Date(Date.UTC(year, month, 0));

    await this.prisma.holiday.deleteMany({
      where: {
        date: {
          gte: startDate,
          lte: endDate
        },
        isDefaultWeekly: true
      }
    });

    const created: Holiday[] = [];
    const cursor = new Date(startDate);

    while (cursor <= endDate) {
      const dayName = getDayName(cursor).toUpperCase();

      if (weekDays.includes(dayName)) {
        const existing = await this.prisma.holiday.findUnique({
          where: { date: new Date(cursor) }
        });

        if (!existing) {
          created.push(
            await this.prisma.holiday.create({
              data: {
                date: new Date(cursor),
                day: capitalizeDay(dayName),
                occasion,
                isDefaultWeekly: true,
                isActive: true
              }
            })
          );
        }
      }

      cursor.setUTCDate(cursor.getUTCDate() + 1);
    }

    return created.map(mapHoliday);
  }

  async getAllHolidays(): Promise<Record<string, unknown>[]> {
    const holidays = await this.prisma.holiday.findMany({
      orderBy: { date: "asc" }
    });

    return holidays.map(mapHoliday);
  }

  async getHolidaysByMonth(year: number, month: number): Promise<Record<string, unknown>[]> {
    if (!year || !month || month < 1 || month > 12) {
      throw new HttpError(400, "Valid year and month are required");
    }

    const startDate = new Date(Date.UTC(year, month - 1, 1));
    const endDate = new Date(Date.UTC(year, month, 0));

    const holidays = await this.prisma.holiday.findMany({
      where: {
        date: {
          gte: startDate,
          lte: endDate
        }
      },
      orderBy: { date: "asc" }
    });

    return holidays.map(mapHoliday);
  }

  async getUpcomingHolidays(): Promise<Record<string, unknown>[]> {
    const today = startOfUtcDay(new Date());

    const holidays = await this.prisma.holiday.findMany({
      where: {
        date: {
          gte: today
        }
      },
      orderBy: { date: "asc" },
      take: 20
    });

    return holidays.map(mapHoliday);
  }

  async getHolidayById(id: number): Promise<Record<string, unknown>> {
    const holiday = await this.findHolidayOrThrow(id);
    return mapHoliday(holiday);
  }

  async updateHoliday(id: number, requestDto: HolidayRequestDto): Promise<Record<string, unknown>> {
    const holiday = await this.findHolidayOrThrow(id);
    const date = requireHolidayDate(requestDto.date);
    const occasion = requestDto.occasion?.trim();

    if (!occasion) {
      throw new HttpError(400, "Occasion is required");
    }

    if (holiday.date.getTime() !== date.getTime()) {
      await ensureUniqueHolidayDate(this.prisma, date);
    }

    const updatedHoliday = await this.prisma.holiday.update({
      where: { id: BigInt(id) },
      data: {
        date,
        day: getDayName(date),
        occasion
      }
    });

    return mapHoliday(updatedHoliday);
  }

  async deleteHoliday(id: number): Promise<void> {
    await this.findHolidayOrThrow(id);
    await this.prisma.holiday.delete({
      where: { id: BigInt(id) }
    });
  }

  async toggleHolidayStatus(id: number): Promise<void> {
    const holiday = await this.findHolidayOrThrow(id);
    await this.prisma.holiday.update({
      where: { id: BigInt(id) },
      data: { isActive: !holiday.isActive }
    });
  }

  private async findHolidayOrThrow(id: number): Promise<Holiday> {
    const holiday = await this.prisma.holiday.findUnique({
      where: { id: BigInt(id) }
    });

    if (!holiday) {
      throw new HttpError(404, `Holiday not found with ID: ${id}`);
    }

    return holiday;
  }
}

function mapHoliday(holiday: Holiday): Record<string, unknown> {
  return {
    id: Number(holiday.id),
    date: holiday.date,
    day: holiday.day,
    occasion: holiday.occasion,
    isDefaultWeekly: holiday.isDefaultWeekly,
    isActive: holiday.isActive
  };
}

async function ensureUniqueHolidayDate(prisma: PrismaClient, date: Date): Promise<void> {
  const existingHoliday = await prisma.holiday.findUnique({
    where: { date }
  });

  if (existingHoliday) {
    throw new HttpError(400, `Holiday already exists for date: ${toDateKey(date)}`);
  }
}

function requireHolidayDate(value?: string): Date {
  if (!value) {
    throw new HttpError(400, "Date is required");
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    throw new HttpError(400, "Invalid date");
  }

  return startOfUtcDay(date);
}

function startOfUtcDay(value: Date): Date {
  return new Date(Date.UTC(value.getUTCFullYear(), value.getUTCMonth(), value.getUTCDate()));
}

function getDayName(date: Date): string {
  return date.toLocaleDateString("en-US", {
    weekday: "long",
    timeZone: "UTC"
  });
}

function capitalizeDay(dayName: string): string {
  return dayName.charAt(0) + dayName.slice(1).toLowerCase();
}

function toDateKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}
