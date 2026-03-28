export interface LoginRequestDto {
  employeeId: string;
  password: string;
}

export interface LoginResponseDto {
  accessToken: string;
  refreshToken: string;
  role: string;
  employeeId: string;
}

export interface ManualRegisterRequestDto {
  employeeId: string;
  password: string;
  role: string;
  email?: string;
}

export interface OtpRequestDto {
  employeeId: string;
  email: string;
}

export interface OtpVerifyRequestDto {
  employeeId: string;
  otp: string;
}

export interface ResetPasswordRequestDto {
  employeeId: string;
  newPassword: string;
}

export interface RefreshRequestDto {
  refreshToken: string;
}

export interface ForgotTicketRequestDto {
  employeeId: string;
  name: string;
  designation?: string;
  department?: string;
  email?: string;
}

