export interface AttendancePayloadDto {
  clockInTime?: string | null;
  clockInLocation?: string | null;
  clockInWorkingFrom?: string | null;
  clockOutTime?: string | null;
  clockOutLocation?: string | null;
  clockOutWorkingFrom?: string | null;
  late?: boolean;
  halfDay?: boolean;
}

export interface BulkAttendanceRequestDto {
  employeeIds?: string[];
  dates?: string[];
  payload?: AttendancePayloadDto;
  overwrite?: boolean;
  markedBy?: string | null;
}

export interface MonthAttendanceRequestDto {
  year?: number;
  month?: number;
  employeeIds?: string[];
  payload?: AttendancePayloadDto;
  overwrite?: boolean;
  markedBy?: string | null;
}

export interface LeaveApplyDto {
  leaveType?: "CASUAL" | "SICK" | "EARNED";
  durationType?: "FULL_DAY" | "MULTIPLE" | "HALF_DAY" | "SECOND_HALF";
  startDate?: string | null;
  endDate?: string | null;
  singleDate?: string | null;
  reason?: string | null;
  documentUrls?: string[] | null;
}

export interface LeaveStatusUpdateDto {
  status?: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  rejectionReason?: string | null;
}

export interface AdminLeaveApplyDto extends LeaveApplyDto {
  employeeIds?: string[];
  status?: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
}
