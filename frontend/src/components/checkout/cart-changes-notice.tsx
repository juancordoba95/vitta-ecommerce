import type { CartChange } from "@/lib/checkout/cart-changes";
import { formatPrice } from "@/lib/format";

function describe(change: CartChange): string {
  switch (change.kind) {
    case "price":
      return `el precio cambió de ${formatPrice(change.previousPrice)} a ${formatPrice(change.currentPrice)}.`;
    case "quantity":
      return change.availableStock === 1
        ? `queda 1 sola unidad (pediste ${change.previousQuantity}): ajustamos la cantidad.`
        : `quedan ${change.availableStock} unidades (pediste ${change.previousQuantity}): ajustamos la cantidad.`;
    case "unavailable":
      return change.reason === "NOT_FOUND"
        ? "ya no está disponible y no se incluye en el pedido."
        : "se quedó sin stock y no se incluye en el pedido.";
  }
}

/**
 * What changed in the cart since the customer last saw it. The order is not sent until
 * she reviews it and confirms again.
 */
export function CartChangesNotice({ changes }: { changes: CartChange[] }) {
  return (
    <div
      role="alert"
      tabIndex={-1}
      id="checkout-changes"
      className="flex flex-col gap-3 rounded-soft bg-butter/70 px-4 py-4 text-sm"
    >
      <p className="text-[0.65rem] font-normal tracking-label uppercase">
        Hubo cambios en tu pedido
      </p>
      <ul className="flex list-disc flex-col gap-1.5 pl-5">
        {changes.map((change) => (
          <li key={`${change.variantId}-${change.kind}`}>
            <span className="font-normal">
              {change.productName} (talle {change.size})
            </span>
            : {describe(change)}
          </li>
        ))}
      </ul>
      <p>Revisá el resumen y, si está todo bien, volvé a confirmar.</p>
    </div>
  );
}
