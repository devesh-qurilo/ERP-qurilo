export interface UserRecord {
  id: number;
  employeeId: string;
  password: string;
  role: string;
  email: string | null;
  active: boolean;
  otp: string | null;
  otpExpiry: Date | null;
}

export interface RefreshTokenRecord {
  id: number;
  token: string;
  employeeId: string;
  expiry: Date;
  active: boolean;
}

