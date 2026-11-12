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

type RequestOptions = {
  query?: QueryParams;
  /** Serialized as JSON. */
  body?: unknown;
  /** Aborts the request (combined with the timeout). */
  signal?: AbortSignal;
};

/**
 * Sends a request to the backend API at NEXT_PUBLIC_API_URL and parses the JSON response.
 * Throws {@link ApiError} on missing configuration, network failures, timeouts and
 * non-2xx responses. An abort through `signal` rethrows the original AbortError.
 */
async function apiRequest<T>(
  method: "GET" | "POST",
  path: string,
  { query, body, signal }: RequestOptions = {},
): Promise<T> {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL;
  if (!baseUrl) {
    throw new ApiError("NEXT_PUBLIC_API_URL is not set");
  }

  const url = new URL(path, baseUrl);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined) url.searchParams.set(key, value);
  }

  const timeout = AbortSignal.timeout(REQUEST_TIMEOUT_MS);
  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers: {
        Accept: "application/json",
        ...(body !== undefined && { "Content-Type": "application/json" }),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
    });
  } catch (cause) {
    if (signal?.aborted) throw cause;
    throw new ApiError(`Could not reach the API at ${url}`, undefined, {
      cause,
    });
  }

  if (!response.ok) {
    const problem = (await response
      .json()
      .catch(() => null)) as ProblemDetail | null;
    throw new ApiError(
      problem?.detail ?? `${method} ${url} failed with ${response.status}`,
      response.status,
    );
  }

  return (await response.json()) as T;
}

/** GET a JSON resource from the backend API. See {@link apiRequest}. */
export function apiGet<T>(path: string, query?: QueryParams): Promise<T> {
  return apiRequest<T>("GET", path, { query });
}

/** POST a JSON body to the backend API. See {@link apiRequest}. */
export function apiPost<T>(
  path: string,
  body: unknown,
  signal?: AbortSignal,
): Promise<T> {
  return apiRequest<T>("POST", path, { body, signal });
}
