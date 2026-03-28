import type { IncomingMessage, ServerResponse } from "node:http";

import { readJsonBody, sendJson } from "../../common/json.js";
import type { TranslateRequestDto } from "./dto.js";
import { TranslateService } from "../../services/translate.service.js";

export async function handleTranslateRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  translateService: TranslateService
): Promise<boolean> {
  if (request.method !== "POST" || request.url !== "/auth/translate") {
    return false;
  }

  try {
    const body = await readJsonBody<TranslateRequestDto>(request);
    const translatedText = await translateService.translate(body.text, body.targetLang);
    sendJson(response, 200, { translatedText });
  } catch {
    sendJson(response, 500, { error: "Translation failed" });
  }

  return true;
}

