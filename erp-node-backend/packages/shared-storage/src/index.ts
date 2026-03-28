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

export class CloudinaryStorageAdapter implements StorageService {
  constructor(private readonly config: CloudinaryConfig) {}

  async upload(input: UploadInput): Promise<UploadResult> {
    const objectKey = `${input.folder}/${input.fileName}`;

    return {
      provider: "cloudinary",
      objectKey,
      publicUrl: this.getPublicUrl(objectKey)
    };
  }

  async delete(_objectKey: string): Promise<void> {
    void this.config;
  }

  getPublicUrl(objectKey: string): string {
    return `https://res.cloudinary.com/${this.config.cloudName}/raw/upload/${objectKey}`;
  }
}

