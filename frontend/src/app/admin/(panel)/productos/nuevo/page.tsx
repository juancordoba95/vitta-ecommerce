import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { AdminUnauthorizedError } from "@/lib/admin/admin-fetch";
import { SESSION_EXPIRED_PATH } from "@/lib/admin/auth";
import { getAdminCategories } from "@/lib/admin/admin-api";
import { NewProductForm } from "@/components/admin/new-product-form";

export const metadata: Metadata = { title: "Nuevo producto" };

export default async function NewProductPage() {
  let categories;
  try {
    categories = await getAdminCategories();
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return (
      <section className="flex flex-col gap-6">
        <PageHeader />
        <p className="rounded-soft border border-charcoal/10 px-5 py-8 text-center text-sm text-charcoal/70">
          No pudimos cargar las categorías. Recargá la página.
        </p>
      </section>
    );
  }

  return (
    <section className="flex flex-col gap-6">
      <PageHeader />
      <NewProductForm categories={categories} />
    </section>
  );
}

function PageHeader() {
  return (
    <div className="flex items-center gap-4">
      <Link
        href="/admin/productos"
        className="text-xs text-charcoal/50 underline-offset-2 hover:text-charcoal hover:underline"
      >
        ← Productos
      </Link>
      <h1 className="text-xs font-normal tracking-title uppercase">Nuevo producto</h1>
    </div>
  );
}
