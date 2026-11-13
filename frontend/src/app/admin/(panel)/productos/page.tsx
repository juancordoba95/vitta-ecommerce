import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { redirect } from "next/navigation";
import { AdminUnauthorizedError } from "@/lib/admin/admin-fetch";
import { SESSION_EXPIRED_PATH } from "@/lib/admin/auth";
import { getAdminProducts, getAdminCategories } from "@/lib/admin/admin-api";
import { ProductsFilter } from "@/components/admin/products-filter";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Productos" };

type Props = {
  searchParams: Promise<{ q?: string; categoryId?: string; page?: string }>;
};

export default async function AdminProductsPage({ searchParams }: Props) {
  const sp = await searchParams;
  const q = sp.q?.trim() ?? "";
  const categoryId = sp.categoryId ? parseInt(sp.categoryId) : undefined;
  const currentPage = sp.page ? Math.max(1, parseInt(sp.page)) : 1;
  const apiPage = currentPage - 1; // API is 0-indexed

  let productsPage, categories;
  try {
    [productsPage, categories] = await Promise.all([
      getAdminProducts({ q: q || undefined, categoryId, page: apiPage, size: 20 }),
      getAdminCategories(),
    ]);
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    return (
      <section className="flex flex-col gap-4">
        <PageHeader />
        <p className="rounded-soft border border-charcoal/10 px-5 py-8 text-center text-sm text-charcoal/70">
          No pudimos cargar los productos. Recargá la página.
        </p>
      </section>
    );
  }

  const { content, totalPages, totalElements } = productsPage;

  function buildHref(page: number) {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (categoryId) params.set("categoryId", String(categoryId));
    if (page > 1) params.set("page", String(page));
    const qs = params.toString();
    return `/admin/productos${qs ? `?${qs}` : ""}`;
  }

  return (
    <section className="flex flex-col gap-6">
      <PageHeader />

      <ProductsFilter
        categories={categories}
        initialQ={q}
        initialCategoryId={categoryId}
      />

      {content.length === 0 ? (
        <p className="rounded-soft border border-dashed border-charcoal/20 px-5 py-10 text-center text-sm text-charcoal/70">
          {q || categoryId ? "No se encontraron productos con esos filtros." : "No hay productos aún."}
        </p>
      ) : (
        <>
          <p className="text-xs text-charcoal/50">
            {totalElements} producto{totalElements !== 1 ? "s" : ""}
          </p>

          {/* Mobile: card list */}
          <div className="flex flex-col gap-2 md:hidden">
            {content.map((p) => (
              <Link
                key={p.id}
                href={`/admin/productos/${p.id}`}
                className="flex items-center gap-3 rounded-soft border border-charcoal/10 bg-white/40 px-3 py-3 transition-colors hover:border-charcoal/30"
              >
                <div className="relative h-14 w-14 shrink-0 overflow-hidden rounded-soft bg-peach/20">
                  {p.imageUrl ? (
                    <Image
                      src={p.imageUrl}
                      alt={p.name}
                      fill
                      className="object-cover"
                      sizes="56px"
                      unoptimized={p.imageUrl.includes("placehold.co")}
                    />
                  ) : (
                    <span className="flex h-full items-center justify-center text-xs text-charcoal/30">Sin foto</span>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{p.name}</p>
                  <p className="text-xs text-charcoal/60">{p.category.name}</p>
                  <p className="text-xs">{formatPrice(p.price)}</p>
                </div>
                <StatusBadge active={p.active} />
              </Link>
            ))}
          </div>

          {/* Desktop: table */}
          <div className="hidden overflow-x-auto rounded-soft border border-charcoal/10 md:block">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-charcoal/10 bg-cream/60">
                  <th className="w-16 px-4 py-3" />
                  <th className="px-4 py-3 text-left text-[0.65rem] tracking-label uppercase font-normal">Nombre</th>
                  <th className="px-4 py-3 text-left text-[0.65rem] tracking-label uppercase font-normal">Precio</th>
                  <th className="px-4 py-3 text-left text-[0.65rem] tracking-label uppercase font-normal">Categoría</th>
                  <th className="px-4 py-3 text-left text-[0.65rem] tracking-label uppercase font-normal">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-charcoal/5">
                {content.map((p) => (
                  <tr key={p.id} className="bg-white/20 transition-colors hover:bg-peach/10">
                    <td className="px-4 py-3">
                      <div className="relative h-10 w-10 overflow-hidden rounded-soft bg-peach/20">
                        {p.imageUrl ? (
                          <Image
                            src={p.imageUrl}
                            alt={p.name}
                            fill
                            className="object-cover"
                            sizes="40px"
                            unoptimized={p.imageUrl.includes("placehold.co")}
                          />
                        ) : null}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <Link
                        href={`/admin/productos/${p.id}`}
                        className="font-medium hover:underline underline-offset-2"
                      >
                        {p.name}
                      </Link>
                    </td>
                    <td className="px-4 py-3 tabular-nums">{formatPrice(p.price)}</td>
                    <td className="px-4 py-3 text-charcoal/70">{p.category.name}</td>
                    <td className="px-4 py-3">
                      <StatusBadge active={p.active} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <nav
              aria-label="Paginación"
              className="flex items-center justify-center gap-4 text-sm"
            >
              {currentPage > 1 ? (
                <Link
                  href={buildHref(currentPage - 1)}
                  className="rounded-full border border-charcoal/20 px-4 py-2 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
                >
                  ← Anterior
                </Link>
              ) : (
                <span className="rounded-full border border-charcoal/10 px-4 py-2 text-[0.65rem] tracking-label uppercase opacity-30">
                  ← Anterior
                </span>
              )}
              <span className="text-xs text-charcoal/60">
                Página {currentPage} de {totalPages}
              </span>
              {currentPage < totalPages ? (
                <Link
                  href={buildHref(currentPage + 1)}
                  className="rounded-full border border-charcoal/20 px-4 py-2 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
                >
                  Siguiente →
                </Link>
              ) : (
                <span className="rounded-full border border-charcoal/10 px-4 py-2 text-[0.65rem] tracking-label uppercase opacity-30">
                  Siguiente →
                </span>
              )}
            </nav>
          )}
        </>
      )}
    </section>
  );
}

function PageHeader() {
  return (
    <div className="flex items-center justify-between gap-4">
      <h1 className="text-xs font-normal tracking-title uppercase">Productos</h1>
      <Link
        href="/admin/productos/nuevo"
        className="rounded-full bg-charcoal px-4 py-2 text-[0.65rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85"
      >
        + Nuevo producto
      </Link>
    </div>
  );
}

function StatusBadge({ active }: { active: boolean }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-[0.6rem] tracking-label uppercase ${
        active ? "bg-peach/50 text-charcoal" : "bg-charcoal/10 text-charcoal/60"
      }`}
    >
      {active ? "Publicado" : "Oculto"}
    </span>
  );
}
