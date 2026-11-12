import { useEffect, useState } from "react";
import { validateCart } from "@/lib/api/cart";
import type {
  CartValidationRequest,
  CartValidationResponse,
  ValidatedCartItem,
} from "@/lib/api/types";
import type { CartItem } from "./cart-items";
import { useCartStore } from "./cart-store";

/** Waits for quantity clicks to settle before asking the server again. */
const DEBOUNCE_MS = 300;

type Outcome =
  | { request: string; response: CartValidationResponse }
  | { request: string; error: true };

export type CartValidation = {
  /**
   * - `empty`: nothing to validate.
   * - `loading`: the current cart has not been validated yet.
   * - `success`: `total` is the server total for the current cart.
   * - `error`: the API failed; the cart shows its last known data.
   */
  status: "empty" | "loading" | "success" | "error";
  /** Server total in ARS; only set when `status` is `success`. */
  total: number | null;
  /** Latest server line per variant (may be from a previous cart while loading). */
  lines: ReadonlyMap<number, ValidatedCartItem>;
  /** Variants whose quantity was lowered to the stock left, with that stock. */
  adjustedStock: Readonly<Record<number, number>>;
  retry: () => void;
};

/**
 * Validates the cart against `POST /api/cart/validate` while mounted and whenever its
 * variants or quantities change, then applies the response to the store (prices, stock,
 * quantities capped to stock). Stale requests are aborted.
 */
export function useCartValidation(items: CartItem[]): CartValidation {
  const applyValidation = useCartStore((state) => state.applyValidation);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const [adjustedStock, setAdjustedStock] = useState<Record<number, number>>({});
  const [attempt, setAttempt] = useState(0);

  // The serialized request doubles as the effect key: it only changes when a variant
  // or a quantity does, not when a price snapshot is refreshed.
  const request =
    items.length > 0
      ? JSON.stringify({
          items: items.map(({ variantId, quantity }) => ({ variantId, quantity })),
        } satisfies CartValidationRequest)
      : null;

  useEffect(() => {
    if (request === null) return;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      validateCart(JSON.parse(request) as CartValidationRequest, controller.signal)
        .then((response) => {
          const adjusted = response.items.filter(
            (line) => line.status === "INSUFFICIENT_STOCK",
          );
          if (adjusted.length > 0) {
            setAdjustedStock((previous) => ({
              ...previous,
              ...Object.fromEntries(
                adjusted.map((line) => [line.variantId, line.availableStock ?? 0]),
              ),
            }));
          }
          setOutcome({ request, response });
          applyValidation(response);
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) return;
          console.warn("[cart] validation failed:", error);
          setOutcome({ request, error: true });
        });
    }, DEBOUNCE_MS);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [request, attempt, applyValidation]);

  const lastResponse = outcome && "response" in outcome ? outcome.response : null;
  const lines = new Map(
    (lastResponse?.items ?? []).map((line) => [line.variantId, line]),
  );
  const retry = () => {
    setOutcome(null);
    setAttempt((n) => n + 1);
  };

  if (request === null) {
    return { status: "empty", total: null, lines, adjustedStock, retry };
  }
  if (outcome?.request !== request) {
    return { status: "loading", total: null, lines, adjustedStock, retry };
  }
  if ("error" in outcome) {
    return { status: "error", total: null, lines, adjustedStock, retry };
  }
  return {
    status: "success",
    total: outcome.response.total,
    lines,
    adjustedStock,
    retry,
  };
}
