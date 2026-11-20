import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { AdminUnauthorizedError } from "@/lib/admin/admin-fetch";
import { SESSION_EXPIRED_PATH } from "@/lib/admin/auth";
import { getAdminCategories } from "@/lib/admin/admin-api";
import { CategoryManager } from "@/components/admin/category-manager";

export const metadata: Metadata = { title: "Categorías" };

export default async function AdminCategoriesPage() {
  let categories;
  try {
    categories = await getAdminCategories();
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    throw error; // backend down: app/admin/error.tsx offers a retry
  }

  return (
    <section className="flex flex-col gap-6">
      <h1 className="text-xs font-normal tracking-title uppercase">Categorías</h1>
      <CategoryManager initialCategories={categories} />
    </section>
  );
}
