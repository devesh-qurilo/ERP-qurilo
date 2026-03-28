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

export interface LeaveApplyDto {
  leaveType?: "CASUAL" | "SICK" | "EARNED";
  durationType?: "FULL_DAY" | "MULTIPLE" | "HALF_DAY" | "SECOND_HALF";
  startDate?: string | null;
  endDate?: string | null;
  singleDate?: string | null;
  reason?: string | null;
}

export interface LeaveStatusUpdateDto {
  status?: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  rejectionReason?: string | null;
}
