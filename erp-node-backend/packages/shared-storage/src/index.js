export class CloudinaryStorageAdapter {
    config;
    constructor(config) {
        this.config = config;
    }
    async upload(input) {
        const objectKey = `${input.folder}/${input.fileName}`;
        return {
            provider: "cloudinary",
            objectKey,
            publicUrl: this.getPublicUrl(objectKey)
        };
    }
    async delete(_objectKey) {
        void this.config;
    }
    getPublicUrl(objectKey) {
        return `https://res.cloudinary.com/${this.config.cloudName}/raw/upload/${objectKey}`;
    }
}
//# sourceMappingURL=index.js.map