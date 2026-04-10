export class HttpError extends Error {
  constructor(
    public readonly statusCode: number,
    message: string,
    public readonly payload?: Record<string, unknown>
  ) {
    super(message);
    this.name = "HttpError";
  }
}
