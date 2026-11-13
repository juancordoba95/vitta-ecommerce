"use server";

import { revalidatePath, updateTag } from "next/cache";
import { redirect } from "next/navigation";
import { adminFetch, AdminUnauthorizedError } from "@/lib/admin/admin-fetch";
import { SESSION_EXPIRED_PATH } from "@/lib/admin/auth";
import { ApiError } from "@/lib/api/client";
import { CATALOG_CACHE_TAG } from "@/lib/api/catalog";
import type { AdminImage, AdminProductDetail, AdminVariant } from "@/lib/admin/admin-types";
import { parsePrice } from "@/lib/parse-price";

export type ActionResult<T = undefined> =
  | { success: true; data: T }
  | { success: false; error: string };

export type FormState = { error: string } | null;

function extractError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 409) {
      const msg = error.message.toLowerCase();
      if (msg.includes("slug")) return "Ya existe un producto con esa URL.";
      if (msg.includes("size") || msg.includes("variant")) return "Ya existe un talle con ese nombre.";
      if (msg.includes("image") || msg.includes("maximum") || msg.includes("8"))
        return "El producto ya tiene el máximo de fotos permitidas (8).";
      return "Ya existe un elemento con ese nombre o URL.";
    }
    if (error.status === 413) return "La imagen supera los 5 MB.";
    if (error.status === 404) return "No se encontró el recurso.";
    return error.message || "Ocurrió un error inesperado.";
  }
  return "Ocurrió un error inesperado. Intentá de nuevo.";
}

type ProductBody = {
  name: string;
  description?: string;
  price: number;
  categoryId: number;
  slug?: string;
};

function parseProductFormData(formData: FormData): ProductBody | { error: string } {
  const name = (formData.get("name") as string)?.trim();
  const description = (formData.get("description") as string)?.trim();
  const priceStr = (formData.get("price") as string)?.trim();
  const categoryIdStr = formData.get("categoryId") as string;
  const slug = (formData.get("slug") as string)?.trim();

  if (!name) return { error: "El nombre es obligatorio." };
  const price = parsePrice(priceStr ?? "");
  if (price === null)
    return { error: "Ingresá un precio válido, por ejemplo 15000 o 15000,50." };
  if (price > 99_999_999.99)
    return { error: "El precio máximo es $99.999.999,99." };
  if (!categoryIdStr || isNaN(parseInt(categoryIdStr)))
    return { error: "Seleccioná una categoría." };

  return {
    name,
    description: description || undefined,
    price,
    categoryId: parseInt(categoryIdStr),
    slug: slug || undefined,
  };
}

/** Creates a product and redirects to its edit page. Returns error state on failure. */
export async function createProductAction(
  _prevState: FormState,
  formData: FormData,
): Promise<FormState> {
  const parsed = parseProductFormData(formData);
  if ("error" in parsed) return parsed;

  let product: AdminProductDetail;
  try {
    product = await adminFetch<AdminProductDetail>("/api/admin/products", {
      method: "POST",
      body: parsed,
    });
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { error: extractError(error) };
  }

  updateTag(CATALOG_CACHE_TAG);
  revalidatePath("/admin/productos");
  redirect(`/admin/productos/${product.id}`);
}

export async function updateProduct(
  id: number,
  body: ProductBody,
): Promise<ActionResult<AdminProductDetail>> {
  try {
    const product = await adminFetch<AdminProductDetail>(`/api/admin/products/${id}`, {
      method: "PUT",
      body,
    });
    updateTag(CATALOG_CACHE_TAG);
    revalidatePath("/admin/productos");
    revalidatePath(`/admin/productos/${id}`);
    return { success: true, data: product };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}

export async function toggleProductActive(
  id: number,
  active: boolean,
): Promise<ActionResult<AdminProductDetail>> {
  try {
    const product = await adminFetch<AdminProductDetail>(`/api/admin/products/${id}/active`, {
      method: "PATCH",
      body: { active },
    });
    updateTag(CATALOG_CACHE_TAG);
    revalidatePath("/admin/productos");
    revalidatePath(`/admin/productos/${id}`);
    return { success: true, data: product };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}

export async function addVariant(
  productId: number,
  size: string,
  stock: number,
): Promise<ActionResult<AdminVariant>> {
  try {
    const variant = await adminFetch<AdminVariant>(
      `/api/admin/products/${productId}/variants`,
      { method: "POST", body: { size, stock } },
    );
    updateTag(CATALOG_CACHE_TAG);
    return { success: true, data: variant };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}

export async function updateVariantStock(
  variantId: number,
  stock: number,
): Promise<ActionResult<AdminVariant>> {
  try {
    const variant = await adminFetch<AdminVariant>(
      `/api/admin/variants/${variantId}/stock`,
      { method: "PATCH", body: { stock } },
    );
    updateTag(CATALOG_CACHE_TAG);
    return { success: true, data: variant };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}

export async function deleteVariant(variantId: number): Promise<ActionResult> {
  try {
    await adminFetch(`/api/admin/variants/${variantId}`, { method: "DELETE" });
    updateTag(CATALOG_CACHE_TAG);
    return { success: true, data: undefined };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}

export async function reorderImages(
  productId: number,
  imageIds: number[],
): Promise<ActionResult<AdminImage[]>> {
  try {
    const images = await adminFetch<AdminImage[]>(
      `/api/admin/products/${productId}/images/order`,
      { method: "PUT", body: { imageIds } },
    );
    updateTag(CATALOG_CACHE_TAG);
    return { success: true, data: images };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}

export async function deleteImage(imageId: number): Promise<ActionResult> {
  try {
    await adminFetch(`/api/admin/images/${imageId}`, { method: "DELETE" });
    updateTag(CATALOG_CACHE_TAG);
    return { success: true, data: undefined };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}
