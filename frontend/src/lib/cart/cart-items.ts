import type { CartValidationResponse } from "@/lib/api/types";

/** Same limit the validation endpoint enforces on `items`. */
export const MAX_CART_ITEMS = 50;
/** Same upper bound the validation endpoint enforces on `quantity`. */
export const MAX_ITEM_QUANTITY = 999;

export type CartItem = {
  variantId: number;
  productSlug: string;
  productName: string;
  size: string;
  imageUrl: string | null;
  /** Snapshot in ARS, for display only: the server price wins once validated. */
  unitPrice: number;
  quantity: number;
  /** Last known stock of the variant; 0 means the item is not available. */
  stock: number;
};

/** What the product page knows when adding a variant. */
export type NewCartItem = Omit<CartItem, "quantity">;

export type AddResult =
  /** The variant was added or its quantity increased. */
  | "added"
  /** The cart already holds all the known stock of the variant. */
  | "stock-limit"
  /** The variant is new and the cart already has {@link MAX_CART_ITEMS} items. */
  | "cart-full";

/** Highest quantity allowed for an item: its known stock, capped by the API limit. */
export function maxQuantity(item: Pick<CartItem, "stock">): number {
  return Math.min(Math.max(item.stock, 0), MAX_ITEM_QUANTITY);
}

export function isAvailable(item: Pick<CartItem, "stock">): boolean {
  return item.stock > 0;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

/**
 * Adds `quantity` units of a variant. Adding a variant already in the cart sums the
 * quantity (never duplicates) and refreshes its snapshot; the result never exceeds the
 * known stock.
 */
export function addItem(
  items: CartItem[],
  newItem: NewCartItem,
  quantity = 1,
): { items: CartItem[]; result: AddResult } {
  const existing = items.find((item) => item.variantId === newItem.variantId);
  const max = maxQuantity(newItem);

  if (existing) {
    const nextQuantity = Math.min(existing.quantity + quantity, max);
    if (nextQuantity <= existing.quantity) {
      return { items, result: "stock-limit" };
    }
    return {
      items: items.map((item) =>
        item === existing ? { ...newItem, quantity: nextQuantity } : item,
      ),
      result: "added",
    };
  }

  if (items.length >= MAX_CART_ITEMS) return { items, result: "cart-full" };
  if (max < 1) return { items, result: "stock-limit" };

  return {
    items: [...items, { ...newItem, quantity: clamp(quantity, 1, max) }],
    result: "added",
  };
}

/** Sets the quantity of an available item, clamped between 1 and its max quantity. */
export function setQuantity(
  items: CartItem[],
  variantId: number,
  quantity: number,
): CartItem[] {
  return items.map((item) =>
    item.variantId === variantId && isAvailable(item)
      ? { ...item, quantity: clamp(Math.trunc(quantity), 1, maxQuantity(item)) }
      : item,
  );
}

export function removeItem(items: CartItem[], variantId: number): CartItem[] {
  return items.filter((item) => item.variantId !== variantId);
}

/**
 * Applies a validation response to the cart:
 * - `OK`: refreshes price and known stock.
 * - `INSUFFICIENT_STOCK`: also lowers the quantity to the available stock.
 * - `OUT_OF_STOCK` / `NOT_FOUND`: keeps the item but marks it unavailable (stock 0).
 *
 * Lines are matched by `variantId`, so a response that arrives after the cart changed
 * only touches the items it knows about and never raises a quantity.
 */
export function applyValidation(
  items: CartItem[],
  response: CartValidationResponse,
): CartItem[] {
  const lines = new Map(response.items.map((line) => [line.variantId, line]));
  return items.map((item) => {
    const line = lines.get(item.variantId);
    if (!line) return item;

    switch (line.status) {
      case "OK":
      case "INSUFFICIENT_STOCK": {
        const stock = line.availableStock ?? item.stock;
        return {
          ...item,
          unitPrice: line.unitPrice ?? item.unitPrice,
          stock,
          quantity: clamp(item.quantity, 1, Math.max(maxQuantity({ stock }), 1)),
        };
      }
      case "OUT_OF_STOCK":
        return { ...item, unitPrice: line.unitPrice ?? item.unitPrice, stock: 0 };
      case "NOT_FOUND":
        return { ...item, stock: 0 };
    }
  });
}

/** Total units in the cart (what the header badge shows). */
export function totalUnits(items: CartItem[]): number {
  return items.reduce((sum, item) => sum + item.quantity, 0);
}

/** Local total from snapshot prices over available items: a fallback when the API fails. */
export function estimatedTotal(items: CartItem[]): number {
  return items
    .filter(isAvailable)
    .reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
}

function isCartItem(value: unknown): value is CartItem {
  if (typeof value !== "object" || value === null) return false;
  const item = value as Record<string, unknown>;
  const isPositiveInt = (v: unknown) => Number.isInteger(v) && (v as number) > 0;
  return (
    isPositiveInt(item.variantId) &&
    isPositiveInt(item.quantity) &&
    Number.isInteger(item.stock) &&
    (item.stock as number) >= 0 &&
    typeof item.unitPrice === "number" &&
    Number.isFinite(item.unitPrice) &&
    typeof item.productSlug === "string" &&
    typeof item.productName === "string" &&
    typeof item.size === "string" &&
    (item.imageUrl === null || typeof item.imageUrl === "string")
  );
}

/**
 * Parses items read from storage, dropping anything malformed, duplicated or beyond
 * the limits, so a tampered or outdated storage entry can't break the cart.
 */
export function sanitizeItems(value: unknown): CartItem[] {
  if (!Array.isArray(value)) return [];
  const items: CartItem[] = [];
  const seen = new Set<number>();
  for (const candidate of value) {
    if (items.length >= MAX_CART_ITEMS) break;
    if (!isCartItem(candidate) || seen.has(candidate.variantId)) continue;
    seen.add(candidate.variantId);
    items.push({
      ...candidate,
      quantity: Math.min(candidate.quantity, MAX_ITEM_QUANTITY),
    });
  }
  return items;
}
