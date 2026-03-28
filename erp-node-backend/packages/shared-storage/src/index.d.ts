export interface UploadInput {
    fileName: string;
    mimeType: string;
    buffer: Buffer;
    folder: string;
}
export interface UploadResult {
    provider: "cloudinary";
    objectKey: string;
    publicUrl: string;
}
export interface StorageService {
    upload(input: UploadInput): Promise<UploadResult>;
    delete(objectKey: string): Promise<void>;
    getPublicUrl(objectKey: string): string;
}
export interface CloudinaryConfig {
    cloudName: string;
    apiKey: string;
    apiSecret: string;
}
export declare class CloudinaryStorageAdapter implements StorageService {
    private readonly config;
    constructor(config: CloudinaryConfig);
    upload(input: UploadInput): Promise<UploadResult>;
    delete(_objectKey: string): Promise<void>;
    getPublicUrl(objectKey: string): string;
}
