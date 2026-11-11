import Image from "next/image";
import type { ProductSummary } from "@/lib/api";
import { formatPrice } from "@/lib/format";

/**
 * placehold.co serves SVG, which the Image Optimization API rejects by default.
 * Next.js recommends `unoptimized` for SVG sources, so only these skip optimization.
 */
function isPlaceholderImage(url: string): boolean {
  try {
    return new URL(url).hostname === "placehold.co";
  } catch {
    return false;
  }
}

export function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <article className="group">
      <div className="relative aspect-4/5 overflow-hidden rounded-soft bg-peach/30">
        {product.imageUrl ? (
          <Image
            src={product.imageUrl}
            alt={product.name}
            fill
            sizes="(min-width: 1024px) 25vw, (min-width: 768px) 33vw, 50vw"
            unoptimized={isPlaceholderImage(product.imageUrl)}
            className="object-cover transition-transform duration-500 group-hover:scale-[1.02]"
          />
        ) : (
          <div className="flex h-full items-center justify-center font-script text-3xl text-charcoal/40">
            Vitta
          </div>
        )}
      </div>
      <h3 className="mt-3 text-[0.7rem] leading-relaxed font-normal tracking-label uppercase">
        {product.name}
      </h3>
      <p className="mt-1 text-sm">{formatPrice(product.price)}</p>
    </article>
  );
}
