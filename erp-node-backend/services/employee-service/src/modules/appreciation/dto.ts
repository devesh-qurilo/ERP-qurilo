export interface AppreciationRequestDto {
  awardId?: number;
  givenToEmployeeId?: string;
  date?: string;
  summary?: string | null;
  photoUrl?: string | null;
}
