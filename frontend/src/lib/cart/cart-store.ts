import { useSyncExternalStore } from "react";
import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";
import type { CartValidationResponse } from "@/lib/api/types";
import {
  addItem,
  applyValidation,
  removeItem,
  sanitizeItems,
  setQuantity,
  type AddResult,
  type CartItem,
  type NewCartItem,
} from "./cart-items";

type CartState = {
  items: CartItem[];
  /** Drawer visibility: UI state, not persisted. */
  isOpen: boolean;
  addItem: (item: NewCartItem, quantity?: number) => AddResult;
  setQuantity: (variantId: number, quantity: number) => void;
  removeItem: (variantId: number) => void;
  applyValidation: (response: CartValidationResponse) => void;
  /** Empties the cart (after an order is sent). */
  clear: () => void;
  open: () => void;
  close: () => void;
};

const STORAGE_KEY = "vitta-cart";

/**
 * Client-side cart persisted in localStorage (only `items`). The state rehydrates
 * synchronously when the module loads in the browser, so anything rendered from it must
 * wait for {@link useHydrated} to avoid hydration mismatches with the server HTML.
 */
export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],
      isOpen: false,
      addItem: (newItem, quantity) => {
        const { items, result } = addItem(get().items, newItem, quantity);
        if (result === "added") set({ items });
        return result;
      },
      setQuantity: (variantId, quantity) =>
        set({ items: setQuantity(get().items, variantId, quantity) }),
      removeItem: (variantId) =>
        set({ items: removeItem(get().items, variantId) }),
      applyValidation: (response) =>
        set({ items: applyValidation(get().items, response) }),
      clear: () => set({ items: [] }),
      open: () => set({ isOpen: true }),
      close: () => set({ isOpen: false }),
    }),
    {
      name: STORAGE_KEY,
      version: 1,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ items: state.items }),
      merge: (persisted, current) => ({
        ...current,
        items: sanitizeItems((persisted as { items?: unknown } | undefined)?.items),
      }),
    },
  ),
);

const noopSubscribe = () => () => {};

/**
 * `false` on the server and during hydration, `true` once mounted in the browser.
 * Gate anything that reads the persisted cart behind it.
 */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    noopSubscribe,
    () => true,
    () => false,
  );
}
