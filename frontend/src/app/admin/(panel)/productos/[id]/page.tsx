import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { AdminUnauthorizedError } from "@/lib/admin/admin-fetch";
import { SESSION_EXPIRED_PATH } from "@/lib/admin/auth";
import { getAdminProduct, getAdminCategories } from "@/lib/admin/admin-api";
import { ApiError } from "@/lib/api/client";
import { ProductForm } from "@/components/admin/product-form";
import { VariantManager } from "@/components/admin/variant-manager";
import { ImageManager } from "@/components/admin/image-manager";
import { PublishSection } from "@/components/admin/publish-section";

export const metadata: Metadata = { title: "Editar producto" };

type Props = { params: Promise<{ id: string }> };

export default async function EditProductPage({ params }: Props) {
  const { id } = await params;
  const productId = parseInt(id);
  if (isNaN(productId)) notFound();

  let product, categories;
  try {
    [product, categories] = await Promise.all([
      getAdminProduct(productId),
      getAdminCategories(),
    ]);
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    if (error instanceof ApiError && error.status === 404) notFound();
    return (
      <section className="flex flex-col gap-6">
        <BackLink />
        <p className="rounded-soft border border-charcoal/10 px-5 py-8 text-center text-sm text-charcoal/70">
          No pudimos cargar el producto. Recargá la página.
        </p>
      </section>
    );
  }

  const hasImages = product.images.length > 0;
  const hasStock = product.variants.some((v) => v.stock > 0);

  return (
    <div className="flex flex-col gap-10">
      <header className="flex flex-wrap items-center gap-3">
        <BackLink />
        <span className="text-charcoal/20">/</span>
        <h1 className="text-xs font-normal tracking-title uppercase truncate max-w-xs">
          {product.name}
        </h1>
      </header>

      {/* Publicación (at the top for quick access on mobile) */}
      <SectionCard title="Publicación">
        <PublishSection
          productId={product.id}
          initialActive={product.active}
          hasImages={hasImages}
          hasStock={hasStock}
        />
      </SectionCard>

      {/* Datos del producto */}
      <SectionCard title="Datos del producto">
        <ProductForm mode="edit" product={product} categories={categories} />
      </SectionCard>

      {/* Talles y stock */}
      <SectionCard title="Talles y stock">
        <VariantManager productId={product.id} initialVariants={product.variants} />
      </SectionCard>

      {/* Fotos */}
      <SectionCard title="Fotos">
        <ImageManager productId={product.id} initialImages={product.images} />
      </SectionCard>
    </div>
  );
}

function BackLink() {
  return (
    <Link
      href="/admin/productos"
      className="text-xs text-charcoal/50 underline-offset-2 hover:text-charcoal hover:underline"
    >
      ← Productos
    </Link>
  );
}

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-[0.65rem] font-normal tracking-title uppercase text-charcoal/70">
        {title}
      </h2>
      <div className="rounded-soft border border-charcoal/10 bg-white/30 px-5 py-5">
        {children}
      </div>
    </section>
  );
}
