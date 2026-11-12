import Link from "next/link";
import { CartButton } from "@/components/cart/cart-button";
import { Wordmark } from "@/components/wordmark";

export function SiteHeader() {
  return (
    <header className="border-b border-charcoal/10">
      {/* The wordmark stays centered; the cart button sits on the right edge. */}
      <div className="relative mx-auto flex max-w-6xl justify-center px-16 py-6">
        <Link href="/" aria-label="Vitta by Marie, ir al inicio">
          <Wordmark />
        </Link>
        <div className="absolute inset-y-0 right-2 flex items-center sm:right-4">
          <CartButton />
        </div>
      </div>
    </header>
  );
}
