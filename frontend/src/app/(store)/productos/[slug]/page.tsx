import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { BackToCatalogLink } from "@/components/back-to-catalog-link";
import { CatalogMessage } from "@/components/catalog/catalog-message";
import { ProductDetail } from "@/components/product/product-detail";
import { ProductDetailSkeleton } from "@/components/product/product-detail-skeleton";
import { getProduct, isValidSlug } from "@/lib/api";
import type { ProductDetail as Product } from "@/lib/api";

const SITE_NAME = "Vitta by Marie";
const MAX_DESCRIPTION_LENGTH = 160;

type LoadResult =
  | { status: "found"; product: Product }
  | { status: "not-found" }
  | { status: "error" };

/** Loads the product without throwing: API failures are logged and reported as "error". */
async function loadProduct(slug: string): Promise<LoadResult> {
  if (!isValidSlug(slug)) return { status: "not-found" };
  try {
    const product = await getProduct(slug);
    return product === null
      ? { status: "not-found" }
      : { status: "found", product };
  } catch (error) {
    console.error("[product] API request failed:", error);
    return { status: "error" };
  }
}

function truncate(text: string, maxLength: number): string {
  return text.length <= maxLength
    ? text
    : `${text.slice(0, maxLength - 1).trimEnd()}…`;
}

export async function generateMetadata({
  params,
}: PageProps<"/productos/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const result = await loadProduct(slug);
  if (result.status !== "found") {
    return { title: SITE_NAME };
  }

  const { product } = result;
  const title = `${product.name} · ${SITE_NAME}`;
  const description = truncate(
    product.description ?? `${product.category.name} de ${SITE_NAME}.`,
    MAX_DESCRIPTION_LENGTH,
  );
  const cover = product.imageUrl ?? product.images[0]?.url;
  const images = cover ? [{ url: cover, alt: product.name }] : undefined;

  return {
    title,
    description,
    openGraph: {
      type: "website",
      siteName: SITE_NAME,
      locale: "es_AR",
      title,
      description,
      images,
    },
    twitter: {
      card: images ? "summary_large_image" : "summary",
      title,
      description,
      images,
    },
  };
}

export default function ProductPage({
  params,
}: PageProps<"/productos/[slug]">) {
  return (
    <div className="mx-auto max-w-6xl px-4 pt-6 sm:pt-10">
      <nav aria-label="Navegación" className="mb-6">
        <BackToCatalogLink />
      </nav>
      {/* params is runtime data (no generateStaticParams): the detail streams in after the static shell. */}
      <Suspense fallback={<ProductDetailSkeleton />}>
        {params.then(({ slug }) => (
          <ProductContent slug={slug} />
        ))}
      </Suspense>
    </div>
  );
}

async function ProductContent({ slug }: { slug: string }) {
  const result = await loadProduct(slug);

  if (result.status === "not-found") notFound();
  if (result.status === "error") {
    return (
      <CatalogMessage
        title="No pudimos cargar este producto"
        description="Estamos acomodando la vidriera. Volvé en un ratito 🤍"
        showAllLink
      />
    );
  }

  return <ProductDetail product={result.product} />;
}
