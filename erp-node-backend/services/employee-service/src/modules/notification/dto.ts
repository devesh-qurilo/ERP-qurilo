export interface SendNotificationDto {
  receiverEmployeeId?: string;
  title?: string;
  message?: string;
  type?: string | null;
}

export interface SendNotificationManyDto {
  receiverEmployeeIds?: string[];
  title?: string;
  message?: string;
  type?: string | null;
}
