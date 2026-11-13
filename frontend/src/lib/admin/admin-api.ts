import "server-only";
import { adminFetch } from "./admin-fetch";
import type { AdminCategory, AdminProductDetail, AdminProductPage } from "./admin-types";

type ProductListParams = {
  q?: string;
  categoryId?: number;
  page?: number;
  size?: number;
};

export async function getAdminProducts(params: ProductListParams = {}): Promise<AdminProductPage> {
  const search = new URLSearchParams();
  if (params.q) search.set("q", params.q);
  if (params.categoryId !== undefined) search.set("categoryId", String(params.categoryId));
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));
  const qs = search.toString();
  return adminFetch<AdminProductPage>(`/api/admin/products${qs ? `?${qs}` : ""}`);
}

export async function getAdminProduct(id: number): Promise<AdminProductDetail> {
  return adminFetch<AdminProductDetail>(`/api/admin/products/${id}`);
}

export async function getAdminCategories(): Promise<AdminCategory[]> {
  return adminFetch<AdminCategory[]>("/api/admin/categories");
}
