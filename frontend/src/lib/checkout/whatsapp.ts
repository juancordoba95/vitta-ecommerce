import { formatPrice } from "@/lib/format";
import type { DeliveryMethod } from "./checkout-form";

/** One line of the order, with the server price at the moment of confirming. */
export type OrderLine = {
  productName: string;
  size: string;
  quantity: number;
  /** Current unit price in ARS. */
  unitPrice: number;
};

export type OrderCustomer = {
  name: string;
  delivery: DeliveryMethod;
  /** Only used for shipping. */
  address?: string;
  /** Only used for shipping. */
  locality?: string;
  notes?: string;
};

export type Order = {
  /** Short order code, e.g. `VT-4F7K`. */
  code: string;
  lines: OrderLine[];
  /** Server total in ARS. */
  total: number;
  customer: OrderCustomer;
};

const DELIVERY_LABELS: Record<DeliveryMethod, string> = {
  pickup: "Retiro",
  shipping: "Envío a domicilio",
};

/** International number without "+": country code first, 10 to 15 digits (E.164). */
const WHATSAPP_NUMBER_PATTERN = /^[1-9]\d{9,14}$/;

/**
 * Normalizes a WhatsApp number to the digits wa.me expects: drops spaces, dashes,
 * parentheses and a leading "+". Returns `null` when the result is not a plausible
 * international number.
 */
export function normalizeWhatsAppNumber(raw: string | undefined): string | null {
  if (!raw) return null;
  const digits = raw.trim().replace(/[\s()\-]/g, "").replace(/^\+/, "");
  return WHATSAPP_NUMBER_PATTERN.test(digits) ? digits : null;
}

/**
 * Store number from `NEXT_PUBLIC_WHATSAPP_NUMBER` (inlined at build time), or `null`
 * when it is missing or malformed. The number only lives in environment variables.
 */
export function getWhatsAppNumber(): string | null {
  return normalizeWhatsAppNumber(process.env.NEXT_PUBLIC_WHATSAPP_NUMBER);
}

/** Collapses whitespace (including line breaks) so a field fits on one line. */
function singleLine(value: string | undefined): string {
  return (value ?? "").replace(/\s+/g, " ").trim();
}

/** Keeps the customer's line breaks but trims each line and drops runs of blank lines. */
function multiLine(value: string | undefined): string {
  return (value ?? "")
    .replace(/\r\n?/g, "\n")
    .split("\n")
    .map((line) => line.replace(/[^\S\n]+/g, " ").trim())
    .join("\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

/** Rounds to cents to avoid floating point noise in subtotals. */
function toCents(amount: number): number {
  return Math.round(amount * 100) / 100;
}

/**
 * Plain-text order message for WhatsApp: greeting, order code, one line per item
 * (name, size, quantity, subtotal), total in ARS, customer details and notes.
 * No WhatsApp markdown, so customer text is never reformatted by accident.
 */
export function buildOrderMessage(order: Order): string {
  const { customer } = order;
  const lines: string[] = [
    "¡Hola Vitta! Quiero hacer este pedido:",
    "",
    `Código de pedido: ${order.code}`,
    "",
    ...order.lines.map(
      (line) =>
        `• ${singleLine(line.productName)} · Talle ${singleLine(line.size)} · ` +
        `x${line.quantity} · ${formatPrice(toCents(line.unitPrice * line.quantity))}`,
    ),
    "",
    `Total: ${formatPrice(toCents(order.total))}`,
    "",
    `Nombre: ${singleLine(customer.name)}`,
    `Entrega: ${DELIVERY_LABELS[customer.delivery]}`,
  ];

  if (customer.delivery === "shipping") {
    lines.push(
      `Dirección: ${singleLine(customer.address)}`,
      `Localidad: ${singleLine(customer.locality)}`,
    );
  }

  const notes = multiLine(customer.notes);
  if (notes) lines.push("", "Notas:", notes);

  return lines.join("\n");
}

/** `https://wa.me/{number}?text={message}` with the message URI-encoded. */
export function buildWhatsAppUrl(number: string, message: string): string {
  return `https://wa.me/${number}?text=${encodeURIComponent(message)}`;
}
