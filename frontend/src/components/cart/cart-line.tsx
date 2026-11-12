"use client";

import Image from "next/image";
import Link from "next/link";
import type { CartItemStatus } from "@/lib/api/types";
import { isAvailable, maxQuantity, type CartItem } from "@/lib/cart/cart-items";
import { useCartStore } from "@/lib/cart/cart-store";
import { formatPrice } from "@/lib/format";
import { isPlaceholderImage } from "@/lib/images";

type CartLineProps = {
  item: CartItem;
  /** Latest server status for this variant, if any. */
  status?: CartItemStatus;
  /** Set when the quantity was lowered to the stock left. */
  adjustedTo?: number;
  onNavigate: () => void;
};

const stepperButton =
  "flex h-9 w-9 items-center justify-center text-base transition-opacity hover:opacity-60 disabled:cursor-not-allowed disabled:opacity-25 disabled:hover:opacity-25";

export function CartLine({ item, status, adjustedTo, onNavigate }: CartLineProps) {
  const setQuantity = useCartStore((state) => state.setQuantity);
  const removeItem = useCartStore((state) => state.removeItem);
  const available = isAvailable(item);
  const max = maxQuantity(item);
  const href = `/productos/${item.productSlug}`;

  return (
    <li className="flex gap-4 py-5">
      <Link
        href={href}
        onClick={onNavigate}
        tabIndex={-1}
        aria-hidden="true"
        className={`relative block aspect-4/5 w-18 shrink-0 overflow-hidden rounded-soft bg-peach/30 ${
          available ? "" : "opacity-40 grayscale"
        }`}
      >
        {item.imageUrl ? (
          <Image
            src={item.imageUrl}
            alt=""
            fill
            sizes="72px"
            unoptimized={isPlaceholderImage(item.imageUrl)}
            className="object-cover"
          />
        ) : (
          <span className="flex h-full items-center justify-center font-script text-lg text-charcoal/40">
            Vitta
          </span>
        )}
      </Link>

      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex items-start justify-between gap-3">
          <Link
            href={href}
            onClick={onNavigate}
            className={`text-[0.7rem] leading-relaxed font-normal tracking-label uppercase hover:opacity-60 ${
              available ? "" : "text-charcoal/50"
            }`}
          >
            {item.productName}
          </Link>
          <button
            type="button"
            onClick={() => removeItem(item.variantId)}
            aria-label={`Quitar ${item.productName}, talle ${item.size}`}
            className="shrink-0 border-b border-charcoal/30 pb-0.5 text-[0.6rem] tracking-label text-charcoal/70 uppercase hover:border-charcoal hover:text-charcoal"
          >
            Quitar
          </button>
        </div>
        <p className="text-xs text-charcoal/70">Talle {item.size}</p>
        <p className={`text-sm ${available ? "" : "text-charcoal/50 line-through"}`}>
          {formatPrice(item.unitPrice)}
        </p>

        {available ? (
          <div className="mt-1 flex flex-col gap-1">
            <div
              role="group"
              aria-label={`Cantidad de ${item.productName}, talle ${item.size}`}
              className="flex w-fit items-center rounded-full border border-charcoal/20"
            >
              <button
                type="button"
                onClick={() => setQuantity(item.variantId, item.quantity - 1)}
                disabled={item.quantity <= 1}
                aria-label="Restar una unidad"
                className={stepperButton}
              >
                −
              </button>
              <span aria-live="polite" className="min-w-6 text-center text-sm tabular-nums">
                {item.quantity}
              </span>
              <button
                type="button"
                onClick={() => setQuantity(item.variantId, item.quantity + 1)}
                disabled={item.quantity >= max}
                aria-label="Sumar una unidad"
                className={stepperButton}
              >
                +
              </button>
            </div>
            {adjustedTo !== undefined ? (
              <p role="status" className="text-xs text-charcoal/80">
                {adjustedTo === 1
                  ? "Queda 1 sola unidad: ajustamos la cantidad."
                  : `Quedan ${adjustedTo} unidades: ajustamos la cantidad.`}
              </p>
            ) : (
              item.quantity >= max && (
                <p className="text-xs text-charcoal/60">No hay más unidades de este talle.</p>
              )
            )}
          </div>
        ) : (
          <div className="mt-1 flex flex-col items-start gap-2">
            <p className="text-[0.65rem] font-normal tracking-label uppercase">
              {status === "NOT_FOUND" ? "Ya no está disponible" : "Sin stock"}
            </p>
            <button
              type="button"
              onClick={() => removeItem(item.variantId)}
              className="rounded-full border border-charcoal/30 px-4 py-2 text-[0.6rem] tracking-label uppercase hover:border-charcoal"
            >
              Quitar del carrito
            </button>
          </div>
        )}
      </div>
    </li>
  );
}
