import { cacheLife, cacheTag } from "next/cache";
import { ApiError, apiGet } from "./client";
import type { Category, ProductDetail, ProductSummary } from "./types";

/** Cache tag for catalog data; revalidate it when the admin changes the catalog. */
export const CATALOG_CACHE_TAG = "catalog";

const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;

/** Same rule the backend enforces, so malformed slugs never reach the API. */
export function isValidSlug(slug: string): boolean {
  return SLUG_PATTERN.test(slug);
}

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

/**
 * Active product detail, or null if the product doesn't exist or is inactive.
 * The 404 is resolved inside the cache scope on purpose: errors thrown from
 * 'use cache' reach callers redacted in production, so a missing product could not
 * be told apart from an outage. Any other failure still throws.
 */
export async function getProduct(slug: string): Promise<ProductDetail | null> {
  "use cache";
  cacheLife("minutes");
  cacheTag(CATALOG_CACHE_TAG);
  try {
    return await apiGet<ProductDetail>(
      `/api/products/${encodeURIComponent(slug)}`,
    );
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}
