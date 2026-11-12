"use client";

import Link from "next/link";
import { useEffect } from "react";
import { HeartIcon } from "@/components/icons";
import { useCartStore } from "@/lib/cart/cart-store";
import { claimPendingOrder, usePendingOrder } from "@/lib/checkout/pending-order";

const primaryButton =
  "flex h-12 w-full items-center justify-center rounded-full bg-charcoal px-8 text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85";

/**
 * "¡Pedido listo!" screen. The first time it shows a freshly confirmed order it empties
 * the cart and opens WhatsApp in the same tab (not a popup, so mobile browsers allow it
 * even though it happens after the checkout's await). Coming back to it never repeats
 * either; "Abrir WhatsApp de nuevo" is a regular link for when it did not open.
 */
export function OrderConfirmation() {
  const order = usePendingOrder();
  const clearCart = useCartStore((state) => state.clear);
  const code = order?.code;

  useEffect(() => {
    if (code === undefined) return;
    const claimed = claimPendingOrder();
    if (claimed === null) return;
    clearCart();
    window.location.assign(claimed.whatsappUrl);
  }, [code, clearCart]);

  if (order === undefined) {
    // Not mounted yet: the order lives in sessionStorage.
    return <div aria-busy="true" className="min-h-[60vh]" />;
  }

  if (order === null) {
    return (
      <section className="mx-auto flex max-w-sm flex-col items-center gap-4 px-4 py-20 text-center sm:py-28">
        <HeartIcon className="text-charcoal/50" />
        <h1 className="text-xs leading-relaxed font-normal tracking-label uppercase">
          No encontramos un pedido reciente
        </h1>
        <p className="text-sm text-charcoal/70">
          Armá tu pedido desde el catálogo y envialo por WhatsApp.
        </p>
        <Link href="/" className={`${primaryButton} mt-4 w-auto`}>
          Ver catálogo
        </Link>
      </section>
    );
  }

  return (
    <section className="mx-auto flex max-w-md flex-col items-center gap-4 px-4 py-16 text-center sm:py-24">
      <HeartIcon className="text-charcoal/50" />
      <h1 className="font-script text-5xl">¡Pedido listo!</h1>
      <p className="mt-2 text-[0.65rem] font-normal tracking-label text-charcoal/70 uppercase">
        Tu código de pedido
      </p>
      <p className="rounded-soft bg-vitta-gradient px-6 py-3 text-2xl tracking-label tabular-nums">
        {order.code}
      </p>
      <p className="text-xs text-charcoal/70">
        Guardalo para consultar por tu pedido por WhatsApp.
      </p>
      <p className="mt-2 text-sm leading-relaxed text-charcoal/80">
        Abrimos WhatsApp con tu pedido. Enviá el mensaje y te confirmamos stock, pago y
        entrega por ahí.
      </p>
      <div className="mt-4 flex w-full flex-col items-center gap-4 sm:max-w-xs">
        <a
          href={order.whatsappUrl}
          target="_blank"
          rel="noopener noreferrer"
          className={primaryButton}
        >
          Abrir WhatsApp de nuevo
        </a>
        <Link
          href="/"
          className="border-b border-charcoal/40 pb-0.5 text-[0.7rem] tracking-label uppercase hover:border-charcoal"
        >
          Volver al catálogo
        </Link>
      </div>
    </section>
  );
}
