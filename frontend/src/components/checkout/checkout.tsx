"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useLayoutEffect, useRef, useState, type FormEvent } from "react";
import { CartLine } from "@/components/cart/cart-line";
import { CartTotal } from "@/components/cart/cart-total";
import { CartValidationError } from "@/components/cart/cart-validation-error";
import { HeartIcon } from "@/components/icons";
import { validateCart } from "@/lib/api/cart";
import type { CartValidationRequest } from "@/lib/api/types";
import { estimatedTotal, isAvailable, type CartItem } from "@/lib/cart/cart-items";
import { useCartStore, useHydrated } from "@/lib/cart/cart-store";
import { useCartValidation } from "@/lib/cart/use-cart-validation";
import { detectCartChanges, type CartChange } from "@/lib/checkout/cart-changes";
import {
  CHECKOUT_FIELD_ORDER,
  EMPTY_CHECKOUT_FORM,
  hasErrors,
  validateCheckoutForm,
  type CheckoutField,
  type CheckoutFormValues,
} from "@/lib/checkout/checkout-form";
import { generateOrderCode } from "@/lib/checkout/order-code";
import { savePendingOrder } from "@/lib/checkout/pending-order";
import {
  buildOrderMessage,
  buildWhatsAppUrl,
  getWhatsAppNumber,
  type OrderCustomer,
} from "@/lib/checkout/whatsapp";
import { CartChangesNotice } from "./cart-changes-notice";
import { CheckoutFields, fieldFocusId } from "./checkout-fields";

const CONFIRMATION_PATH = "/checkout/confirmacion";

const SUBMIT_ERRORS = {
  config:
    "No podemos enviar pedidos por WhatsApp en este momento: falta configurar el número de la tienda. Escribinos por Instagram y lo resolvemos 🤍",
  api: "No pudimos verificar precios y stock. Revisá tu conexión y volvé a intentar.",
  cartChanged: "Tu carrito cambió mientras lo verificábamos. Revisalo y volvé a confirmar.",
  nothingAvailable: "No hay productos disponibles en tu carrito para enviar.",
} as const;

type SubmitError = keyof typeof SUBMIT_ERRORS;

function toRequest(items: CartItem[]): CartValidationRequest {
  return { items: items.map(({ variantId, quantity }) => ({ variantId, quantity })) };
}

/**
 * Opens an empty tab to be pointed at wa.me once the order is confirmed. Must run
 * synchronously inside the submit so popup blockers allow it; `null` when blocked.
 */
function openBlankTab(): Window | null {
  const tab = window.open("", "_blank");
  if (tab === null) return null;
  try {
    // Same as rel="noopener": wa.me must not be able to reach back into the store.
    tab.opener = null;
    tab.document.title = "Abriendo WhatsApp…";
    tab.document.body.textContent = "Abriendo WhatsApp…";
  } catch {
    // Cosmetic only.
  }
  return tab;
}

function toCustomer(values: CheckoutFormValues): OrderCustomer {
  const delivery = values.delivery === "shipping" ? "shipping" : "pickup";
  return {
    name: values.name,
    delivery,
    ...(delivery === "shipping" && { address: values.address, locality: values.locality }),
    notes: values.notes,
  };
}

/** `/checkout`: order summary validated against the API plus the customer form. */
export function Checkout() {
  const hydrated = useHydrated();
  const items = useCartStore((state) => state.items);

  // The cart lives in the browser: nothing to show until mounted.
  if (!hydrated) return <CheckoutSkeleton />;
  if (items.length === 0) return <EmptyCheckout />;
  return <CheckoutContent items={items} />;
}

function CheckoutContent({ items }: { items: CartItem[] }) {
  const router = useRouter();
  const validation = useCartValidation(items);
  const applyValidation = useCartStore((state) => state.applyValidation);

  const [values, setValues] = useState<CheckoutFormValues>(EMPTY_CHECKOUT_FORM);
  // Errors stay hidden until the first submit; then they update as she types.
  const [showErrors, setShowErrors] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<SubmitError | null>(null);
  const [changes, setChanges] = useState<CartChange[]>([]);
  // Activity keeps this page mounted (hidden) after navigating away: once an order is
  // sent, leave the form clean for the next visit.
  const shouldReset = useRef(false);

  useLayoutEffect(
    () => () => {
      if (!shouldReset.current) return;
      shouldReset.current = false;
      setValues(EMPTY_CHECKOUT_FORM);
      setShowErrors(false);
      setSubmitting(false);
      setSubmitError(null);
      setChanges([]);
    },
    [],
  );

  // Bring the list of changes into view so she reviews it before confirming again.
  useEffect(() => {
    if (changes.length === 0) return;
    const notice = document.getElementById("checkout-changes");
    notice?.scrollIntoView({ behavior: "smooth", block: "center" });
    notice?.focus({ preventScroll: true });
  }, [changes]);

  const errors = showErrors ? validateCheckoutForm(values) : {};
  const orderItems = items.filter(isAvailable);
  const hasUnavailable = orderItems.length < items.length;

  function handleChange(field: CheckoutField, value: string) {
    setValues((previous) => ({ ...previous, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    setSubmitError(null);
    setChanges([]);

    const formErrors = validateCheckoutForm(values);
    if (hasErrors(formErrors)) {
      setShowErrors(true);
      const first = CHECKOUT_FIELD_ORDER.find((field) => formErrors[field]);
      if (first) document.getElementById(fieldFocusId(first))?.focus();
      return;
    }

    const number = getWhatsAppNumber();
    if (number === null) {
      console.error("[checkout] NEXT_PUBLIC_WHATSAPP_NUMBER is missing or malformed");
      setSubmitError("config");
      return;
    }
    if (orderItems.length === 0) {
      setSubmitError("nothingAvailable");
      return;
    }

    // Open WhatsApp's tab now, inside the click: after the await below the browser
    // would treat window.open() as an unrequested popup and block it.
    const tab = openBlankTab();
    function abort() {
      tab?.close();
      setSubmitting(false);
    }

    // Revalidate right before sending: stock and prices may have changed.
    const request = toRequest(orderItems);
    setSubmitting(true);
    let response;
    try {
      response = await validateCart(request);
    } catch (error) {
      console.warn("[checkout] validation failed:", error);
      setSubmitError("api");
      abort();
      return;
    }

    const current = useCartStore.getState().items.filter(isAvailable);
    if (JSON.stringify(toRequest(current)) !== JSON.stringify(request)) {
      setSubmitError("cartChanged");
      abort();
      return;
    }

    const detected = detectCartChanges(orderItems, response);
    applyValidation(response);
    if (detected.length > 0) {
      setChanges(detected);
      abort();
      return;
    }

    const byVariant = new Map(orderItems.map((item) => [item.variantId, item]));
    const code = generateOrderCode();
    const message = buildOrderMessage({
      code,
      total: response.total,
      lines: response.items.map((line) => {
        const item = byVariant.get(line.variantId);
        return {
          productName: line.productName ?? item?.productName ?? "",
          size: line.size ?? item?.size ?? "",
          quantity: line.quantity,
          unitPrice: line.unitPrice ?? item?.unitPrice ?? 0,
        };
      }),
      customer: toCustomer(values),
    });

    // The store tab moves to the confirmation screen, which offers an "Abrir WhatsApp"
    // link when the browser blocked the new tab anyway.
    const whatsappUrl = buildWhatsAppUrl(number, message);
    if (tab !== null) tab.location.href = whatsappUrl;
    savePendingOrder({ code, whatsappUrl, opened: tab !== null });
    shouldReset.current = true;
    router.replace(CONFIRMATION_PATH);
  }

  return (
    <div className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_minmax(0,26rem)] lg:items-start lg:gap-14">
      <section
        aria-labelledby="checkout-summary-title"
        className="rounded-soft border border-charcoal/10 bg-white/40 lg:sticky lg:top-8 lg:order-2"
      >
        <h2
          id="checkout-summary-title"
          className="border-b border-charcoal/10 px-5 py-4 text-xs font-normal tracking-title uppercase"
        >
          Tu pedido
        </h2>
        {validation.status === "error" && (
          <CartValidationError onRetry={validation.retry} className="mx-5 mt-4" />
        )}
        <ul className="divide-y divide-charcoal/10 px-5 lg:max-h-[55vh] lg:overflow-y-auto lg:overscroll-contain">
          {items.map((item) => (
            <CartLine
              key={item.variantId}
              item={item}
              status={validation.lines.get(item.variantId)?.status}
              adjustedTo={validation.adjustedStock[item.variantId]}
              onNavigate={() => {}}
            />
          ))}
        </ul>
        <div className="flex flex-col gap-2 border-t border-charcoal/10 px-5 py-5">
          <CartTotal validation={validation} estimated={estimatedTotal(items)} />
          {hasUnavailable && (
            <p className="text-xs text-charcoal/60">
              Los productos sin stock no se suman al total ni se incluyen en el pedido.
            </p>
          )}
        </div>
      </section>

      <form
        noValidate
        onSubmit={handleSubmit}
        aria-labelledby="checkout-form-title"
        className="flex flex-col gap-8 lg:order-1"
      >
        <h2
          id="checkout-form-title"
          className="text-xs font-normal tracking-title uppercase"
        >
          Tus datos
        </h2>

        <CheckoutFields
          values={values}
          errors={errors}
          disabled={submitting}
          onChange={handleChange}
        />

        {changes.length > 0 && <CartChangesNotice changes={changes} />}

        <div className="flex flex-col gap-3 pb-[env(safe-area-inset-bottom)]">
          {submitError && (
            <p role="alert" className="rounded-soft bg-butter/70 px-4 py-3 text-sm">
              {SUBMIT_ERRORS[submitError]}
            </p>
          )}
          <button
            type="submit"
            disabled={submitting || orderItems.length === 0}
            aria-describedby="checkout-submit-hint"
            className="h-12 w-full rounded-full bg-charcoal text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-35 sm:max-w-sm"
          >
            {submitting ? "Verificando tu pedido…" : "Confirmar pedido"}
          </button>
          <p id="checkout-submit-hint" className="text-xs text-charcoal/60 sm:max-w-sm">
            Vamos a abrir WhatsApp en una pestaña nueva con tu pedido listo para enviar. Ahí te confirmamos
            stock, pago y entrega.
          </p>
        </div>
      </form>
    </div>
  );
}

function EmptyCheckout() {
  return (
    <div className="mx-auto flex max-w-sm flex-col items-center gap-3 py-16 text-center">
      <HeartIcon className="text-charcoal/50" />
      <p className="text-xs leading-relaxed font-normal tracking-label uppercase">
        Tu carrito está vacío
      </p>
      <p className="text-sm text-charcoal/70">Elegí lo que más te guste de la colección.</p>
      <Link
        href="/"
        className="mt-4 rounded-full bg-charcoal px-8 py-3.5 text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85"
      >
        Ver catálogo
      </Link>
    </div>
  );
}

function CheckoutSkeleton() {
  return (
    <div
      aria-busy="true"
      aria-label="Cargando tu pedido"
      className="grid animate-pulse gap-10 lg:grid-cols-[minmax(0,1fr)_minmax(0,26rem)] lg:gap-14"
    >
      <div className="h-64 rounded-soft bg-peach/30 lg:order-2" />
      <div className="flex flex-col gap-6 lg:order-1">
        <div className="h-12 rounded-soft bg-peach/30" />
        <div className="h-20 rounded-soft bg-peach/30" />
        <div className="h-24 rounded-soft bg-peach/30" />
        <div className="h-12 rounded-full bg-peach/30 sm:max-w-sm" />
      </div>
    </div>
  );
}
