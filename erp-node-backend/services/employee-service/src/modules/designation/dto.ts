export interface DesignationCreateDto {
  designationName?: string;
  parentDesignationId?: number | null;
}

export interface DesignationUpdateDto {
  designationName?: string;
  parentDesignationId?: number | null;
}
