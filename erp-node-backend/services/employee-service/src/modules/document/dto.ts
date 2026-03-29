export interface EmployeeDocumentUploadDto {
  bucket?: string | null;
  path?: string | null;
  filename?: string;
  mime?: string | null;
  size?: number | null;
  url?: string;
  uploadedBy?: string | null;
}
