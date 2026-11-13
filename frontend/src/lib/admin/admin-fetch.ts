import "server-only";
import { ApiError } from "@/lib/api/client";
import type { ProblemDetail } from "@/lib/api/types";
import { BACKEND_TIMEOUT_MS, backendUrl } from "./backend";
import { getAdminToken } from "./session";

/** No session cookie, or the backend rejected the token (401/403): log in again. */
export class AdminUnauthorizedError extends Error {
  constructor(readonly status?: number) {
    super("Admin session missing, expired or invalid");
    this.name = "AdminUnauthorizedError";
  }
}

type AdminFetchOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  /** Serialized as JSON. */
  body?: unknown;
};

/**
 * Calls an admin endpoint of the backend from the Next server with
 * `Authorization: Bearer <token>` taken from the session cookie, and parses the JSON
 * response (`undefined` for 204). Throws {@link AdminUnauthorizedError} on a missing
 * cookie, 401 or 403, and {@link ApiError} on any other failure.
 */
export async function adminFetch<T>(
  path: string,
  { method = "GET", body }: AdminFetchOptions = {},
): Promise<T> {
  const token = await getAdminToken();
  if (!token) throw new AdminUnauthorizedError();

  const url = backendUrl(path);
  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
        ...(body !== undefined && { "Content-Type": "application/json" }),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      cache: "no-store",
      signal: AbortSignal.timeout(BACKEND_TIMEOUT_MS),
    });
  } catch (cause) {
    throw new ApiError(`Could not reach the API at ${url}`, undefined, { cause });
  }

  if (response.status === 401 || response.status === 403) {
    throw new AdminUnauthorizedError(response.status);
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as ProblemDetail | null;
    throw new ApiError(
      problem?.detail ?? `${method} ${url} failed with ${response.status}`,
      response.status,
    );
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
