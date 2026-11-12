import Link from "next/link";
import type { ProductDetail as Product } from "@/lib/api";
import { formatPrice } from "@/lib/format";
import { ProductGallery } from "./product-gallery";
import { ProductPurchase } from "./product-purchase";

/** Gallery URLs in position order, falling back to the cover if there is no gallery. */
function galleryUrls(product: Product): string[] {
  if (product.images.length > 0) {
    return [...product.images]
      .sort((a, b) => a.position - b.position)
      .map((image) => image.url);
  }
  return product.imageUrl ? [product.imageUrl] : [];
}

/** Mobile-first layout: gallery on top, info below; side by side from md up. */
export function ProductDetail({ product }: { product: Product }) {
  return (
    <article className="grid gap-8 md:grid-cols-2 md:gap-12">
      <ProductGallery images={galleryUrls(product)} productName={product.name} />

      <div className="flex flex-col gap-6 md:pt-4">
        <div className="flex flex-col gap-3">
          <Link
            href={`/?category=${encodeURIComponent(product.category.slug)}`}
            className="self-start text-[0.65rem] tracking-label text-charcoal/70 uppercase hover:text-charcoal"
          >
            {product.category.name}
          </Link>
          <h1 className="text-lg leading-relaxed font-normal tracking-label uppercase sm:text-xl">
            {product.name}
          </h1>
          <p className="text-xl">{formatPrice(product.price)}</p>
        </div>

        <ProductPurchase
          product={{
            slug: product.slug,
            name: product.name,
            price: product.price,
            imageUrl: product.imageUrl ?? product.images[0]?.url ?? null,
          }}
          variants={product.variants}
        />

        {product.description && (
          <div className="border-t border-charcoal/10 pt-6">
            <h2 className="text-[0.7rem] font-normal tracking-label uppercase">
              Descripción
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-charcoal/80">
              {product.description}
            </p>
          </div>
        )}
      </div>
    </article>
  );
}
