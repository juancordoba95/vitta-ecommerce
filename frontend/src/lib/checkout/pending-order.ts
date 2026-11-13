import { useMemo, useSyncExternalStore } from "react";
import { ORDER_CODE_PATTERN } from "./order-code";

/** The last confirmed order, kept for the confirmation screen. */
export type PendingOrder = {
  code: string;
  whatsappUrl: string;
  /** Whether checkout managed to open WhatsApp in a new tab (popup not blocked). */
  opened: boolean;
  /**
   * `false` until the confirmation screen has emptied the cart once, so going back to
   * it (or a reload) never repeats it.
   */
  handled: boolean;
};

const STORAGE_KEY = "vitta-last-order";
const CHANGE_EVENT = "vitta-last-order-change";

function readRaw(): string | null {
  try {
    return sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function write(order: PendingOrder): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(order));
  } catch (error) {
    console.warn("[checkout] could not store the order:", error);
  }
  window.dispatchEvent(new Event(CHANGE_EVENT));
}

/** Parses a stored order; anything tampered with (e.g. a non wa.me URL) is ignored. */
function parse(raw: string | null): PendingOrder | null {
  if (raw === null) return null;
  try {
    const value = JSON.parse(raw) as Partial<PendingOrder>;
    if (
      typeof value.code === "string" &&
      ORDER_CODE_PATTERN.test(value.code) &&
      typeof value.whatsappUrl === "string" &&
      value.whatsappUrl.startsWith("https://wa.me/") &&
      typeof value.opened === "boolean" &&
      typeof value.handled === "boolean"
    ) {
      return {
        code: value.code,
        whatsappUrl: value.whatsappUrl,
        opened: value.opened,
        handled: value.handled,
      };
    }
  } catch {
    // Malformed JSON: treat as no order.
  }
  return null;
}

/** Stores a freshly confirmed order (in sessionStorage: it dies with the tab). */
export function savePendingOrder(order: Omit<PendingOrder, "handled">): void {
  write({ ...order, handled: false });
}

/**
 * Marks the stored order as handled. Returns `true` only the first time, so the
 * caller runs its one-off side effect (empty the cart) exactly once.
 */
export function claimPendingOrder(): PendingOrder | null {
  const order = parse(readRaw());
  if (order === null || order.handled) return null;
  write({ ...order, handled: true });
  return order;
}

function subscribe(onChange: () => void): () => void {
  window.addEventListener(CHANGE_EVENT, onChange);
  window.addEventListener("storage", onChange);
  return () => {
    window.removeEventListener(CHANGE_EVENT, onChange);
    window.removeEventListener("storage", onChange);
  };
}

/**
 * The stored order: `undefined` on the server and during hydration (not known yet),
 * `null` when there is none.
 */
export function usePendingOrder(): PendingOrder | null | undefined {
  const raw = useSyncExternalStore<string | null | undefined>(
    subscribe,
    readRaw,
    () => undefined,
  );
  return useMemo(() => (raw === undefined ? undefined : parse(raw)), [raw]);
}
