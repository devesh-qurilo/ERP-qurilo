export interface DepartmentCreateDto {
  departmentName?: string;
  parentDepartmentId?: number | null;
}

export interface DepartmentUpdateDto {
  departmentName?: string;
  parentDepartmentId?: number | null;
}
