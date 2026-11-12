import type { CartValidationResponse } from "@/lib/api/types";
import type { CartItem } from "@/lib/cart/cart-items";

type ChangedItem = Pick<CartItem, "variantId" | "productName" | "size">;

/** Something that changed between what the customer saw and the server's answer. */
export type CartChange =
  | (ChangedItem & { kind: "price"; previousPrice: number; currentPrice: number })
  | (ChangedItem & { kind: "quantity"; previousQuantity: number; availableStock: number })
  | (ChangedItem & { kind: "unavailable"; reason: "OUT_OF_STOCK" | "NOT_FOUND" });

/**
 * Compares the items the customer is about to order with a fresh validation response:
 * price changes, quantities above the stock left and items that are no longer
 * available. An empty list means the order can be sent as shown.
 */
export function detectCartChanges(
  items: CartItem[],
  response: CartValidationResponse,
): CartChange[] {
  const lines = new Map(response.items.map((line) => [line.variantId, line]));
  const changes: CartChange[] = [];

  for (const item of items) {
    const line = lines.get(item.variantId);
    const base = {
      variantId: item.variantId,
      productName: item.productName,
      size: item.size,
    };

    if (!line || line.status === "NOT_FOUND") {
      changes.push({ ...base, kind: "unavailable", reason: "NOT_FOUND" });
      continue;
    }
    if (line.status === "OUT_OF_STOCK") {
      changes.push({ ...base, kind: "unavailable", reason: "OUT_OF_STOCK" });
      continue;
    }
    if (line.unitPrice !== null && line.unitPrice !== item.unitPrice) {
      changes.push({
        ...base,
        kind: "price",
        previousPrice: item.unitPrice,
        currentPrice: line.unitPrice,
      });
    }
    if (line.status === "INSUFFICIENT_STOCK") {
      changes.push({
        ...base,
        kind: "quantity",
        previousQuantity: item.quantity,
        availableStock: line.availableStock ?? 0,
      });
    }
  }

  return changes;
}
