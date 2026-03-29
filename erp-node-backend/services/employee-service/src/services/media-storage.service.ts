import { randomUUID } from "node:crypto";

import { CloudinaryStorageAdapter, type StorageService } from "@erp/shared-storage";

import { HttpError } from "../common/errors.js";

interface UploadFileInput {
  filename: string | null;
  contentType: string | null;
  data: Buffer;
}

export class MediaStorageService {
  private readonly storage: StorageService | null;

  constructor(cloudName: string | null, apiKey: string | null, apiSecret: string | null, private readonly rootFolder: string) {
    this.storage =
      cloudName && apiKey && apiSecret
        ? new CloudinaryStorageAdapter({
            cloudName,
            apiKey,
            apiSecret
          })
        : null;
  }

  async saveUploadedFile(file: UploadFileInput | null | undefined, folder: string): Promise<{ url: string; objectKey: string | null } | null> {
    if (!file || !file.data.length) {
      return null;
    }

    if (!this.storage) {
      throw new HttpError(400, "Cloudinary is not configured for file uploads");
    }

    const extension = inferExtension(file.filename, file.contentType);
    const fileName = `${randomUUID()}${extension}`;
    const uploaded = await this.storage.upload({
      fileName,
      mimeType: file.contentType ?? "application/octet-stream",
      buffer: file.data,
      folder: `${this.rootFolder}/${folder}`.replace(/\/+/g, "/")
    });

    return {
      url: uploaded.publicUrl,
      objectKey: uploaded.objectKey
    };
  }
}

function inferExtension(filename: string | null, contentType: string | null): string {
  const fromName = filename?.includes(".") ? filename.slice(filename.lastIndexOf(".")) : "";

  if (fromName) {
    return fromName;
  }

  switch (contentType) {
    case "image/png":
      return ".png";
    case "image/jpeg":
      return ".jpg";
    case "image/webp":
      return ".webp";
    default:
      return "";
  }
}
