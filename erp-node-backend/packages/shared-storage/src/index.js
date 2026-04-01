import { createHash } from "node:crypto";

export class CloudinaryStorageAdapter {
    config;
    constructor(config) {
        this.config = config;
    }
    async upload(input) {
        const timestamp = Math.floor(Date.now() / 1000);
        const resourceType = resolveResourceType(input.mimeType);
        const publicId = `${normalizePath(input.folder)}/${stripExtension(input.fileName)}`;
        const signature = signParams({
            public_id: publicId,
            timestamp
        }, this.config.apiSecret);
        const form = new FormData();
        form.append("file", new Blob([new Uint8Array(input.buffer)], { type: input.mimeType }), input.fileName);
        form.append("api_key", this.config.apiKey);
        form.append("timestamp", String(timestamp));
        form.append("public_id", publicId);
        form.append("signature", signature);
        const response = await fetch(`https://api.cloudinary.com/v1_1/${this.config.cloudName}/${resourceType}/upload`, {
            method: "POST",
            body: form
        });
        if (!response.ok) {
            throw new Error(`Cloudinary upload failed with status ${response.status}`);
        }
        const payload = await response.json();
        if (!payload.secure_url || !payload.public_id || !payload.resource_type) {
            throw new Error("Cloudinary upload response missing required fields");
        }
        const objectKey = `${payload.resource_type}:${payload.public_id}`;
        return {
            provider: "cloudinary",
            objectKey,
            publicUrl: payload.secure_url
        };
    }
    async delete(objectKey) {
        const [resourceType, ...publicIdParts] = objectKey.split(":");
        const publicId = publicIdParts.join(":");
        if (!resourceType || !publicId) {
            return;
        }
        const timestamp = Math.floor(Date.now() / 1000);
        const signature = signParams({
            invalidate: true,
            public_id: publicId,
            timestamp
        }, this.config.apiSecret);
        const form = new FormData();
        form.append("api_key", this.config.apiKey);
        form.append("timestamp", String(timestamp));
        form.append("public_id", publicId);
        form.append("invalidate", "true");
        form.append("signature", signature);
        const response = await fetch(`https://api.cloudinary.com/v1_1/${this.config.cloudName}/${resourceType}/destroy`, {
            method: "POST",
            body: form
        });
        if (!response.ok) {
            throw new Error(`Cloudinary delete failed with status ${response.status}`);
        }
    }
    getPublicUrl(objectKey) {
        const [resourceType, ...publicIdParts] = objectKey.split(":");
        const publicId = publicIdParts.join(":");
        return `https://res.cloudinary.com/${this.config.cloudName}/${resourceType || "raw"}/upload/${publicId}`;
    }
}
function signParams(params, apiSecret) {
    const serialized = Object.entries(params)
        .filter(([, value]) => value !== undefined && value !== null && value !== "")
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([key, value]) => `${key}=${value}`)
        .join("&");
    return createHash("sha1")
        .update(`${serialized}${apiSecret}`)
        .digest("hex");
}
function normalizePath(value) {
    return value.replace(/^\/+|\/+$/g, "").replace(/\/+/g, "/");
}
function stripExtension(fileName) {
    const normalized = fileName.trim().replace(/\s+/g, "-");
    const lastDot = normalized.lastIndexOf(".");
    return lastDot > 0 ? normalized.slice(0, lastDot) : normalized;
}
function resolveResourceType(mimeType) {
    if (mimeType.startsWith("image/")) {
        return "image";
    }
    if (mimeType.startsWith("video/")) {
        return "video";
    }
    return "raw";
}
