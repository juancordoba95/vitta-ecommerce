import { getCategories, getProducts } from "@/lib/api";
import { CatalogMessage } from "./catalog-message";
import { CategoryFilter } from "./category-filter";
import { ProductGrid } from "./product-grid";

type SearchParams = Record<string, string | string[] | undefined>;

function parseCategory(value: SearchParams[string]): string | undefined {
  return typeof value === "string" && value !== "" ? value : undefined;
}

/**
 * Category filter + product grid. Reads `searchParams` (runtime data), so it must be
 * rendered inside <Suspense>. API failures are thrown to app/(store)/error.tsx.
 */
export async function Catalog({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const categorySlug = parseCategory((await searchParams).category);

  // Without a filter both requests run in parallel. With one, the slug is checked
  // against the category list first: errors thrown inside 'use cache' reach us
  // redacted in production (no status), so a backend 404 can't be told apart from
  // an outage, and an unknown slug shouldn't trigger a failing request.
  const categoriesRequest = getCategories();
  const unfilteredProductsRequest =
    categorySlug === undefined ? getProducts() : null;

  const categories = await categoriesRequest;

  const filter = (
    <CategoryFilter categories={categories} activeSlug={categorySlug} />
  );

  const isUnknownCategory =
    categorySlug !== undefined &&
    !categories.some((category) => category.slug === categorySlug);

  if (isUnknownCategory) {
    return (
      <>
        {filter}
        <CatalogMessage
          title="No encontramos esa categoría"
          description="Puede que ya no esté disponible. Mirá todo lo que tenemos para vos."
          showAllLink
        />
      </>
    );
  }

  const products = await (unfilteredProductsRequest ?? getProducts(categorySlug));

  return (
    <>
      {filter}
      <div className="mt-10">
        {products.length === 0 ? (
          <CatalogMessage
            title="Todavía no hay productos acá"
            description="Estamos preparando novedades. Volvé pronto 🤍"
            showAllLink={categorySlug !== undefined}
          />
        ) : (
          <ProductGrid products={products} />
        )}
      </div>
    </>
  );
}
