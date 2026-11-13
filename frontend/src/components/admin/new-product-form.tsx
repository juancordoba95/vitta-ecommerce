"use client";

import Link from "next/link";
import { useActionState } from "react";
import { createProductAction } from "@/app/admin/(panel)/productos/actions";
import { CreateFormFields } from "@/components/admin/product-form";
import type { AdminCategory } from "@/lib/admin/admin-types";

type Props = {
  categories: AdminCategory[];
};

/**
 * Client component: wraps the new-product form with useActionState so the
 * server action can redirect on success or return an error without a full
 * page reload on failure.
 */
export function NewProductForm({ categories }: Props) {
  const [state, action, isPending] = useActionState(createProductAction, null);

  return (
    <form action={action} className="flex max-w-lg flex-col gap-6">
      <CreateFormFields categories={categories} />

      {state?.error && (
        <p role="alert" className="rounded-soft bg-butter/70 px-4 py-3 text-sm text-danger">
          {state.error}
        </p>
      )}

      <p className="text-xs text-charcoal/50">
        El producto se crea en modo <strong>Oculto</strong>. Podés publicarlo después de
        agregar fotos y stock.
      </p>

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={isPending}
          className="h-12 flex-1 rounded-full bg-charcoal text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-35 sm:flex-none sm:px-10"
        >
          {isPending ? "Creando…" : "Crear producto"}
        </button>
        <Link
          href="/admin/productos"
          className="flex h-12 items-center rounded-full border border-charcoal/20 px-6 text-[0.7rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
        >
          Cancelar
        </Link>
      </div>
    </form>
  );
}
