"use client";

import { useState } from "react";
import type { ProductVariant } from "@/lib/api";
import {
  MAX_CART_ITEMS,
  remainingQuantity,
  type AddResult,
} from "@/lib/cart/cart-items";
import { useCartStore, useHydrated } from "@/lib/cart/cart-store";
import { isLowStock } from "@/lib/stock";
import { SizeSelector } from "./size-selector";

/** Product fields the cart keeps as a display snapshot. */
export type PurchasableProduct = {
  slug: string;
  name: string;
  /** Price in ARS. */
  price: number;
  imageUrl: string | null;
};

const ALL_UNITS_ADDED = "Ya agregaste todas las unidades disponibles";

const ADD_FEEDBACK: Record<Exclude<AddResult, "added">, string> = {
  "out-of-stock": "Este talle no tiene stock.",
  "stock-limit": ALL_UNITS_ADDED,
  "cart-full": `Tu carrito llegó al máximo de ${MAX_CART_ITEMS} productos distintos.`,
  invalid: "No pudimos agregar este producto. Probá de nuevo.",
};

/** Size selection, stock hint and the "Agregar al carrito" button. */
export function ProductPurchase({
  product,
  variants,
}: {
  product: PurchasableProduct;
  variants: ProductVariant[];
}) {
  const availableVariants = variants.filter((variant) => variant.available);
  const hasStock = availableVariants.length > 0;
  // No size is preselected: the customer must pick one explicitly. Exception: if
  // only one size has stock, there is nothing to choose, so it starts selected.
  const [selected, setSelected] = useState<ProductVariant | null>(() =>
    availableVariants.length === 1 ? availableVariants[0] : null,
  );
  const [feedback, setFeedback] = useState<string | null>(null);
  const addItem = useCartStore((state) => state.addItem);
  const openCart = useCartStore((state) => state.open);
  const hydrated = useHydrated();
  // Units of the selected size that still fit in the cart. The cart is only known after
  // mounting (persisted in the browser); before that the button can't be clicked anyway.
  const remaining = useCartStore((state) =>
    selected === null
      ? 0
      : remainingQuantity(state.items, selected.id, selected.stock),
  );
  const selectedInStock = selected !== null && selected.available && selected.stock > 0;
  const allUnitsAdded = hydrated && selectedInStock && remaining === 0;
  const canAdd = selectedInStock && !allUnitsAdded;

  function handleSelect(variant: ProductVariant) {
    setSelected(variant);
    setFeedback(null);
  }

  function handleAddToCart() {
    if (selected === null || !canAdd) return;
    const result = addItem({
      variantId: selected.id,
      productSlug: product.slug,
      productName: product.name,
      size: selected.size,
      imageUrl: product.imageUrl,
      unitPrice: product.price,
      stock: selected.stock,
    });
    if (result === "added") {
      setFeedback(null);
      openCart();
    } else {
      setFeedback(ADD_FEEDBACK[result]);
    }
  }

  if (variants.length === 0 || !hasStock) {
    return (
      <p className="text-[0.7rem] tracking-label text-charcoal/70 uppercase">
        Sin stock por ahora
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-5">
      <div>
        <SizeSelector
          variants={variants}
          selectedId={selected?.id ?? null}
          onSelect={handleSelect}
        />
        <p aria-live="polite" className="mt-3 min-h-5 text-sm">
          {selected !== null && isLowStock(selected.stock) && (
            <span className="text-[0.7rem] font-normal tracking-label uppercase">
              Últimas unidades
            </span>
          )}
        </p>
      </div>

      <div className="flex flex-col gap-3">
        <button
          type="button"
          onClick={handleAddToCart}
          disabled={!canAdd}
          aria-describedby="add-to-cart-feedback"
          className="h-12 w-full rounded-full bg-charcoal text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-35 sm:max-w-xs"
        >
          Agregar al carrito
        </button>
        <p
          id="add-to-cart-feedback"
          role="status"
          className="text-sm text-charcoal/80 empty:hidden"
        >
          {allUnitsAdded ? ALL_UNITS_ADDED : feedback}
        </p>
      </div>
    </div>
  );
}
