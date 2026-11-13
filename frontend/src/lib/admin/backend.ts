import "server-only";
import { ApiError } from "@/lib/api/client";

export const BACKEND_TIMEOUT_MS = 5000;

/**
 * Absolute URL of a backend API path for server-to-server calls. Reuses
 * NEXT_PUBLIC_API_URL: the backend is reachable at the same URL from the Next server.
 */
export function backendUrl(path: string): URL {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL;
  if (!baseUrl) throw new ApiError("NEXT_PUBLIC_API_URL is not set");
  return new URL(path, baseUrl);
}
