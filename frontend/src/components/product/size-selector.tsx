"use client";

import type { ProductVariant } from "@/lib/api";

type SizeSelectorProps = {
  variants: ProductVariant[];
  selectedId: number | null;
  onSelect: (variant: ProductVariant) => void;
};

/**
 * Size picker built on native radio inputs (keyboard and screen-reader friendly).
 * Out-of-stock sizes are disabled and struck through.
 */
export function SizeSelector({
  variants,
  selectedId,
  onSelect,
}: SizeSelectorProps) {
  return (
    <fieldset>
      <legend className="text-[0.7rem] font-normal tracking-label uppercase">
        Talle
      </legend>
      <div className="mt-3 flex flex-wrap gap-2">
        {variants.map((variant) => (
          <label key={variant.id} className="relative">
            <input
              type="radio"
              name="size"
              value={variant.id}
              checked={variant.id === selectedId}
              disabled={!variant.available}
              onChange={() => onSelect(variant)}
              className="peer sr-only"
            />
            <span
              className={[
                "flex h-11 min-w-11 items-center justify-center rounded-full border px-4 text-sm transition-colors select-none",
                "peer-focus-visible:outline peer-focus-visible:outline-1 peer-focus-visible:outline-offset-3 peer-focus-visible:outline-charcoal",
                variant.available
                  ? "cursor-pointer border-charcoal/20 hover:border-charcoal peer-checked:border-charcoal peer-checked:bg-charcoal peer-checked:text-cream"
                  : "cursor-not-allowed border-charcoal/10 text-charcoal/35 line-through",
              ].join(" ")}
            >
              {variant.size}
              {!variant.available && <span className="sr-only"> (sin stock)</span>}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
