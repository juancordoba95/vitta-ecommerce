import { cacheLife, cacheTag } from "next/cache";
import { apiGet } from "./client";
import type { Category, ProductSummary } from "./types";

/** Cache tag for catalog data; revalidate it when the admin changes the catalog. */
export const CATALOG_CACHE_TAG = "catalog";

export async function getCategories(): Promise<Category[]> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG);
  return apiGet<Category[]>("/api/categories");
}

/** Active products, optionally filtered by category slug. */
export async function getProducts(
  categorySlug?: string,
): Promise<ProductSummary[]> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG);
  return apiGet<ProductSummary[]>("/api/products", {
    category: categorySlug,
  });
}
