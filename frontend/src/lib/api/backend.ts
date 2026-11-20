import "server-only";
import { ApiError } from "./errors";

/**
 * Max wait for the backend. Render's free tier sleeps after 15 minutes idle and the first
 * request takes ~50 s, longer than any Netlify function limit we can rely on (the docs say
 * 60 s but some accounts get less). Giving up early still wakes the backend; the UI then
 * offers a retry.
 */
export const BACKEND_TIMEOUT_MS = 8000;

/**
 * Absolute URL of a backend API path. BACKEND_URL is server-only: the browser never calls
 * the backend directly (it goes through this app's own route handlers).
 */
export function backendUrl(path: string): URL {
  const baseUrl = process.env.BACKEND_URL;
  if (!baseUrl) {
    throw new ApiError(
      "BACKEND_URL is not set. Set it to the backend base URL (e.g. https://<service>.onrender.com) in the server environment.",
    );
  }
  try {
    return new URL(path, baseUrl);
  } catch (cause) {
    throw new ApiError("BACKEND_URL is not a valid absolute URL", undefined, { cause });
  }
}
