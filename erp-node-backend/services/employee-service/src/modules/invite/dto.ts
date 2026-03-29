export interface EmployeeInviteRequestDto {
  email?: string;
  to?: string;
}

export interface CompleteRegistrationRequestDto {
  name?: string;
  mobile?: string | null;
  gender?: string | null;
  birthday?: string | null;
  address?: string | null;
  password?: string;
}
