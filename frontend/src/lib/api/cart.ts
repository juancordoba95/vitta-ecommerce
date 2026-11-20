import { ApiError } from "./errors";
import type { CartValidationRequest, CartValidationResponse } from "./types";

/**
 * Checks a client-side cart against current prices and stock (read-only: nothing is
 * reserved). Runs in the browser, so it goes through this app's own route handler
 * (`/api/cart/validate`), which forwards to the backend; the browser never sees the
 * backend URL. Never cached.
 */
export async function validateCart(
  request: CartValidationRequest,
  signal?: AbortSignal,
): Promise<CartValidationResponse> {
  const response = await fetch("/api/cart/validate", {
    method: "POST",
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
    signal,
  });
  if (!response.ok) {
    throw new ApiError(`Cart validation failed with ${response.status}`, response.status);
  }
  return (await response.json()) as CartValidationResponse;
}
