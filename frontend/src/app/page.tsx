import { Suspense } from "react";
import { HeroBanner } from "@/components/hero-banner";
import { Catalog } from "@/components/catalog/catalog";
import { CatalogSkeleton } from "@/components/catalog/catalog-skeleton";

export default function Home({ searchParams }: PageProps<"/">) {
  return (
    <>
      <HeroBanner />
      <section
        aria-labelledby="catalog-title"
        className="mx-auto max-w-6xl px-4 pt-12 sm:pt-16"
      >
        <h2
          id="catalog-title"
          className="mb-8 text-center text-xs font-normal tracking-title uppercase"
        >
          Nuestra colección
        </h2>
        {/* searchParams is runtime data: the catalog streams in after the static shell. */}
        <Suspense fallback={<CatalogSkeleton />}>
          <Catalog searchParams={searchParams} />
        </Suspense>
      </section>
    </>
  );
}
