export class TranslateService {
  constructor(private readonly apiKey: string) {}

  async translate(text: string, targetLang: string): Promise<string> {
    if (this.apiKey === "disabled") {
      throw new Error("Translate API is not configured");
    }

    void text;
    void targetLang;

    throw new Error("Google Translate integration not implemented yet");
  }
}

