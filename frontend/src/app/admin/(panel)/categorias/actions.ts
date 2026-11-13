"use server";

import { revalidatePath, updateTag } from "next/cache";
import { redirect } from "next/navigation";
import { adminFetch, AdminUnauthorizedError } from "@/lib/admin/admin-fetch";
import { SESSION_EXPIRED_PATH } from "@/lib/admin/auth";
import { ApiError } from "@/lib/api/client";
import { CATALOG_CACHE_TAG } from "@/lib/api/catalog";
import type { AdminCategory } from "@/lib/admin/admin-types";

export type CategoryActionResult =
  | { success: true; data: AdminCategory }
  | { success: false; error: string };

function extractError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 409) return "Ya existe una categoría con esa URL.";
    return error.message || "Ocurrió un error inesperado.";
  }
  return "Ocurrió un error inesperado. Intentá de nuevo.";
}

export async function createCategory(
  name: string,
  slug: string,
): Promise<CategoryActionResult> {
  if (!name.trim()) return { success: false, error: "El nombre es obligatorio." };

  try {
    const category = await adminFetch<AdminCategory>("/api/admin/categories", {
      method: "POST",
      body: { name: name.trim(), slug: slug.trim() || undefined },
    });
    updateTag(CATALOG_CACHE_TAG);
    revalidatePath("/admin/categorias");
    return { success: true, data: category };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}

export async function updateCategory(
  id: number,
  name: string,
  slug: string,
): Promise<CategoryActionResult> {
  if (!name.trim()) return { success: false, error: "El nombre es obligatorio." };

  try {
    const category = await adminFetch<AdminCategory>(`/api/admin/categories/${id}`, {
      method: "PUT",
      body: { name: name.trim(), slug: slug.trim() || undefined },
    });
    updateTag(CATALOG_CACHE_TAG);
    revalidatePath("/admin/categorias");
    return { success: true, data: category };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return { success: false, error: extractError(error) };
  }
}
