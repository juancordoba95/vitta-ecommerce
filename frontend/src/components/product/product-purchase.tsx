"use client";

import { useState } from "react";
import type { ProductVariant } from "@/lib/api";
import { MAX_CART_ITEMS, type AddResult } from "@/lib/cart/cart-items";
import { useCartStore } from "@/lib/cart/cart-store";
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

const ADD_FEEDBACK: Record<Exclude<AddResult, "added">, string> = {
  "stock-limit": "Ya tenés en el carrito todas las unidades de este talle.",
  "cart-full": `Tu carrito llegó al máximo de ${MAX_CART_ITEMS} productos distintos.`,
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

  function handleSelect(variant: ProductVariant) {
    setSelected(variant);
    setFeedback(null);
  }

  function handleAddToCart() {
    if (selected === null) return;
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
          disabled={selected === null}
          className="h-12 w-full rounded-full bg-charcoal text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-35 sm:max-w-xs"
        >
          Agregar al carrito
        </button>
        <p role="status" className="text-sm text-charcoal/80 empty:hidden">
          {feedback}
        </p>
      </div>
    </div>
  );
}
