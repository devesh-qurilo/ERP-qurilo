export interface RegisterPushTokenRequestDto {
  provider?: string;
  token?: string;
  deviceInfo?: string | null;
}

export interface UnregisterPushTokenRequestDto {
  token?: string;
}
