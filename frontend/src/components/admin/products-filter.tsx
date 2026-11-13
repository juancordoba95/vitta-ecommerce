"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import type { AdminCategory } from "@/lib/admin/admin-types";

const inputCls =
  "h-10 rounded-soft border border-charcoal/20 bg-white/70 px-3 text-sm text-charcoal transition-colors hover:border-charcoal/40 focus:border-charcoal";

type Props = {
  categories: AdminCategory[];
  initialQ: string;
  initialCategoryId: number | undefined;
};

export function ProductsFilter({ categories, initialQ, initialCategoryId }: Props) {
  const router = useRouter();
  const [q, setQ] = useState(initialQ);
  const [categoryId, setCategoryId] = useState<string>(
    initialCategoryId !== undefined ? String(initialCategoryId) : "",
  );

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const params = new URLSearchParams();
    if (q.trim()) params.set("q", q.trim());
    if (categoryId) params.set("categoryId", categoryId);
    router.push(`/admin/productos${params.toString() ? `?${params}` : ""}`);
  }

  function handleClear() {
    setQ("");
    setCategoryId("");
    router.push("/admin/productos");
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
      <input
        type="search"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Buscar por nombre…"
        maxLength={150}
        className={`${inputCls} w-full sm:w-56`}
      />
      <select
        value={categoryId}
        onChange={(e) => setCategoryId(e.target.value)}
        className={`${inputCls} w-full sm:w-48`}
      >
        <option value="">Todas las categorías</option>
        {categories.map((c) => (
          <option key={c.id} value={c.id}>{c.name}</option>
        ))}
      </select>
      <button
        type="submit"
        className="h-10 rounded-full bg-charcoal px-5 text-[0.65rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85"
      >
        Buscar
      </button>
      {(q || categoryId) && (
        <button
          type="button"
          onClick={handleClear}
          className="h-10 rounded-full border border-charcoal/20 px-5 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
        >
          Limpiar
        </button>
      )}
    </form>
  );
}
