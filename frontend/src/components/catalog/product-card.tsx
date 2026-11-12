import Image from "next/image";
import Link from "next/link";
import type { ProductSummary } from "@/lib/api";
import { formatPrice } from "@/lib/format";
import { isPlaceholderImage } from "@/lib/images";

export function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <article className="group">
      <Link href={`/productos/${product.slug}`} className="block">
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
      </Link>
    </article>
  );
}
