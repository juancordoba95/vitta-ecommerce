"use client";

import { BagIcon } from "@/components/icons";
import { totalUnits } from "@/lib/cart/cart-items";
import { useCartStore, useHydrated } from "@/lib/cart/cart-store";

const MAX_BADGE = 99;

/** Header button that opens the cart drawer; the badge shows the total units. */
export function CartButton() {
  const hydrated = useHydrated();
  const units = useCartStore((state) => totalUnits(state.items));
  const open = useCartStore((state) => state.open);
  // The persisted cart only exists in the browser: render the badge after mounting so
  // the first client render matches the server HTML.
  const count = hydrated ? units : 0;

  return (
    <button
      type="button"
      onClick={open}
      aria-label={
        count > 0
          ? `Abrir carrito, ${count} ${count === 1 ? "producto" : "productos"}`
          : "Abrir carrito"
      }
      aria-haspopup="dialog"
      className="relative flex h-11 w-11 items-center justify-center rounded-full transition-opacity hover:opacity-60"
    >
      <BagIcon width={22} height={22} />
      {count > 0 && (
        <span
          aria-hidden="true"
          className="absolute top-1 right-0.5 flex h-4.5 min-w-4.5 items-center justify-center rounded-full bg-charcoal px-1 text-[0.6rem] leading-none font-normal text-cream"
        >
          {count > MAX_BADGE ? `${MAX_BADGE}+` : count}
        </span>
      )}
    </button>
  );
}
