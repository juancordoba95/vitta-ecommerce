"use client";

import Link from "next/link";
import { useEffect, useRef } from "react";
import { CloseIcon, HeartIcon } from "@/components/icons";
import { estimatedTotal, isAvailable } from "@/lib/cart/cart-items";
import { useCartStore, useHydrated } from "@/lib/cart/cart-store";
import { useCartValidation } from "@/lib/cart/use-cart-validation";
import { CartLine } from "./cart-line";
import { CartTotal } from "./cart-total";
import { CartValidationError } from "./cart-validation-error";

/**
 * Cart side panel built on a modal <dialog> (focus trap, Escape and inert background for
 * free). Full width on mobile, a right-hand panel from sm up. Its contents only mount
 * while open, so the cart is validated each time it opens.
 */
export function CartDrawer() {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const hydrated = useHydrated();
  const isOpen = useCartStore((state) => state.isOpen);
  const close = useCartStore((state) => state.close);
  const visible = hydrated && isOpen;

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (visible && !dialog.open) dialog.showModal();
    if (!visible && dialog.open) dialog.close();
  }, [visible]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="cart-title"
      // Escape closes the dialog natively; keep the store in sync.
      onClose={close}
      // A click on the dialog element itself (not its contents) is a click on the backdrop.
      onClick={(event) => {
        if (event.target === event.currentTarget) close();
      }}
      className="m-0 ml-auto h-dvh max-h-none w-full max-w-none border-0 bg-cream p-0 text-charcoal transition-transform duration-300 ease-out backdrop:bg-charcoal/40 open:translate-x-0 motion-reduce:transition-none sm:max-w-md sm:shadow-xl starting:open:translate-x-full"
    >
      {visible && <CartContents onClose={close} />}
    </dialog>
  );
}

function CartContents({ onClose }: { onClose: () => void }) {
  const items = useCartStore((state) => state.items);
  const validation = useCartValidation(items);
  const hasUnavailable = items.some((item) => !isAvailable(item));

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between border-b border-charcoal/10 py-3 pr-2 pl-5">
        <h2 id="cart-title" className="text-xs font-normal tracking-title uppercase">
          Tu carrito
        </h2>
        <button
          type="button"
          onClick={onClose}
          aria-label="Cerrar carrito"
          className="flex h-11 w-11 items-center justify-center rounded-full transition-opacity hover:opacity-60"
        >
          <CloseIcon />
        </button>
      </div>

      {items.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-3 px-5 text-center">
          <HeartIcon className="text-charcoal/50" />
          <p className="text-xs leading-relaxed font-normal tracking-label uppercase">
            Tu carrito está vacío
          </p>
          <p className="text-sm text-charcoal/70">Elegí lo que más te guste de la colección.</p>
          <Link
            href="/"
            onClick={onClose}
            className="mt-2 border-b border-charcoal/40 pb-0.5 text-[0.7rem] tracking-label uppercase hover:border-charcoal"
          >
            Ver catálogo
          </Link>
        </div>
      ) : (
        <>
          {validation.status === "error" && (
            <CartValidationError onRetry={validation.retry} className="mx-5 mt-4" />
          )}

          <ul className="flex-1 divide-y divide-charcoal/10 overflow-y-auto overscroll-contain px-5">
            {items.map((item) => (
              <CartLine
                key={item.variantId}
                item={item}
                status={validation.lines.get(item.variantId)?.status}
                adjustedTo={validation.adjustedStock[item.variantId]}
                onNavigate={onClose}
              />
            ))}
          </ul>

          <div className="flex flex-col gap-4 border-t border-charcoal/10 px-5 pt-5 pb-[max(1.25rem,env(safe-area-inset-bottom))]">
            <CartTotal
              validation={validation}
              estimated={estimatedTotal(items)}
            />
            {hasUnavailable && (
              <p className="-mt-2 text-xs text-charcoal/60">
                Los productos sin stock no se suman al total.
              </p>
            )}
            <Link
              href="/checkout"
              onClick={onClose}
              aria-describedby="checkout-hint"
              className="flex h-12 w-full items-center justify-center rounded-full bg-charcoal text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85"
            >
              Finalizar pedido
            </Link>
            <p id="checkout-hint" className="-mt-2 text-center text-xs text-charcoal/60">
              Completás tus datos y nos enviás el pedido por WhatsApp.
            </p>
          </div>
        </>
      )}
    </div>
  );
}
