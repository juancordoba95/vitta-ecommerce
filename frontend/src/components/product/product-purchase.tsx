"use client";

import { useState } from "react";
import type { ProductVariant } from "@/lib/api";
import { isLowStock } from "@/lib/stock";
import { SizeSelector } from "./size-selector";

/** Size selection, stock hint and the "Agregar al carrito" button. */
export function ProductPurchase({ variants }: { variants: ProductVariant[] }) {
  const availableVariants = variants.filter((variant) => variant.available);
  const hasStock = availableVariants.length > 0;
  // No size is preselected: the customer must pick one explicitly. Exception: if
  // only one size has stock, there is nothing to choose, so it starts selected.
  const [selected, setSelected] = useState<ProductVariant | null>(() =>
    availableVariants.length === 1 ? availableVariants[0] : null,
  );

  function handleAddToCart() {
    if (selected === null) return;
    // TODO(M2.3): add { variantId: selected.id, quantity: 1 } to the cart.
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
          onSelect={setSelected}
        />
        <p aria-live="polite" className="mt-3 min-h-5 text-sm">
          {selected !== null && isLowStock(selected.stock) && (
            <span className="text-[0.7rem] font-normal tracking-label uppercase">
              Últimas unidades
            </span>
          )}
        </p>
      </div>

      <button
        type="button"
        onClick={handleAddToCart}
        disabled={selected === null}
        className="h-12 w-full rounded-full bg-charcoal text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-35 sm:max-w-xs"
      >
        Agregar al carrito
      </button>
    </div>
  );
}
