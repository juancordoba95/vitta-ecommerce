import type { Metadata } from "next";
import { BackToCatalogLink } from "@/components/back-to-catalog-link";
import { Checkout } from "@/components/checkout/checkout";

export const metadata: Metadata = {
  title: "Finalizar pedido · Vitta by Marie",
  robots: { index: false },
};

export default function CheckoutPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 pt-6 pb-8 sm:pt-10">
      <nav aria-label="Navegación" className="mb-6">
        <BackToCatalogLink />
      </nav>
      <h1 className="mb-8 text-center text-xs font-normal tracking-title uppercase sm:mb-10">
        Finalizar pedido
      </h1>
      {/* The cart lives in the browser: the summary and form render after mounting. */}
      <Checkout />
    </div>
  );
}
