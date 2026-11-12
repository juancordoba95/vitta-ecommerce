import { apiPost } from "./client";
import type { CartValidationRequest, CartValidationResponse } from "./types";

/**
 * Checks a client-side cart against current prices and stock (read-only: nothing is
 * reserved). Safe to call from the browser; never cached.
 */
export function validateCart(
  request: CartValidationRequest,
  signal?: AbortSignal,
): Promise<CartValidationResponse> {
  return apiPost<CartValidationResponse>("/api/cart/validate", request, signal);
}
