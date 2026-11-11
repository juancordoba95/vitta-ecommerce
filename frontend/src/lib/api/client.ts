import type { ProblemDetail } from "./types";

const REQUEST_TIMEOUT_MS = 5000;

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

type QueryParams = Record<string, string | undefined>;

/**
 * GET a JSON resource from the backend API at NEXT_PUBLIC_API_URL.
 * Throws {@link ApiError} on missing configuration, network failures, timeouts and
 * non-2xx responses.
 */
export async function apiGet<T>(path: string, query?: QueryParams): Promise<T> {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL;
  if (!baseUrl) {
    throw new ApiError("NEXT_PUBLIC_API_URL is not set");
  }

  const url = new URL(path, baseUrl);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined) url.searchParams.set(key, value);
  }

  let response: Response;
  try {
    response = await fetch(url, {
      headers: { Accept: "application/json" },
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
  } catch (cause) {
    throw new ApiError(`Could not reach the API at ${url}`, undefined, {
      cause,
    });
  }

  if (!response.ok) {
    const problem = (await response
      .json()
      .catch(() => null)) as ProblemDetail | null;
    throw new ApiError(
      problem?.detail ?? `GET ${url} failed with ${response.status}`,
      response.status,
    );
  }

  return (await response.json()) as T;
}
