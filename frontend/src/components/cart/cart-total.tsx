import type { CartValidation } from "@/lib/cart/use-cart-validation";
import { formatPrice } from "@/lib/format";

/**
 * Server total of the cart; "Calculando…" while validating and a local "Total
 * estimado" from the snapshot prices when the API fails.
 */
export function CartTotal({
  validation,
  estimated,
}: {
  validation: CartValidation;
  estimated: number;
}) {
  const isError = validation.status === "error";
  return (
    <div
      aria-live="polite"
      aria-busy={validation.status === "loading"}
      className="flex items-baseline justify-between"
    >
      <span className="text-[0.7rem] font-normal tracking-label uppercase">
        {isError ? "Total estimado" : "Total"}
      </span>
      {validation.status === "success" && validation.total !== null ? (
        <span className="text-lg">{formatPrice(validation.total)}</span>
      ) : isError ? (
        <span className="text-lg">{formatPrice(estimated)}</span>
      ) : (
        <span className="text-sm text-charcoal/60">Calculando…</span>
      )}
    </div>
  );
}
