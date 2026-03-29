export interface PromotionRequestDto {
  newDepartmentId?: number;
  newDesignationId?: number;
  sendNotification?: boolean;
  isPromotion?: boolean;
  remarks?: string | null;
}
