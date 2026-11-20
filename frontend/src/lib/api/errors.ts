/** Failure talking to the backend: bad config, network, timeout or a non-2xx response. */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    options?: ErrorOptions,
  ) {
    super(message, options);
    this.name = "ApiError";
  }
}
