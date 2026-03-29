export interface HolidayRequestDto {
  date?: string;
  occasion?: string;
}

export interface BulkHolidayRequestDto {
  holidays?: HolidayRequestDto[];
}

export interface DefaultHolidaysRequestDto {
  weekDays?: string[];
  occasion?: string | null;
  year?: number | null;
  month?: number | null;
}
