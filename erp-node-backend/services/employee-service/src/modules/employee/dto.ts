export interface EmployeeRequestDto {
  employeeId?: string;
  name?: string;
  email?: string;
  password?: string;
  profilePictureUrl?: string;
  gender?: string;
  birthday?: string | null;
  bloodGroup?: string | null;
  joiningDate?: string | null;
  language?: string | null;
  country?: string | null;
  mobile?: string | null;
  address?: string | null;
  about?: string | null;
  departmentId?: number | null;
  designationId?: number | null;
  reportingToId?: string | null;
  role?: string | null;
  loginAllowed?: boolean | null;
  receiveEmailNotification?: boolean | null;
  hourlyRate?: number | null;
  slackMemberId?: string | null;
  skills?: string[] | null;
  probationEndDate?: string | null;
  noticePeriodStartDate?: string | null;
  noticePeriodEndDate?: string | null;
  employmentType?: string | null;
  maritalStatus?: string | null;
  businessAddress?: string | null;
  officeShift?: string | null;
}

export interface EmployeeRoleUpdateDto {
  role?: string;
}
