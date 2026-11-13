"use client";

import { useEffect, useRef, useState } from "react";
import type { AdminCategory, AdminProductDetail } from "@/lib/admin/admin-types";
import { updateProduct } from "@/app/admin/(panel)/productos/actions";
import { parsePrice } from "@/lib/parse-price";

const labelCls = "text-[0.65rem] font-normal tracking-label uppercase";
const inputCls =
  "h-12 w-full rounded-soft border border-charcoal/20 bg-white/70 px-4 text-base text-charcoal transition-colors placeholder:text-charcoal/40 hover:border-charcoal/40 focus:border-charcoal disabled:opacity-60 sm:text-sm";
const textareaCls =
  "w-full rounded-soft border border-charcoal/20 bg-white/70 px-4 py-3 text-base text-charcoal transition-colors placeholder:text-charcoal/40 hover:border-charcoal/40 focus:border-charcoal disabled:opacity-60 sm:text-sm resize-none";

type Props =
  | { mode: "create"; categories: AdminCategory[] }
  | { mode: "edit"; product: AdminProductDetail; categories: AdminCategory[] };

/** Product data form used for both create (new page) and edit (edit page). */
export function ProductForm(props: Props) {
  if (props.mode === "edit") {
    return <EditForm product={props.product} categories={props.categories} />;
  }
  return <CreateFormFields categories={props.categories} />;
}

/** Fields only — used inside the new-product page form (server action via form). */
export function CreateFormFields({ categories }: { categories: AdminCategory[] }) {
  return (
    <div className="flex flex-col gap-5">
      <NameField />
      <DescriptionField />
      <PriceField />
      <CategoryField categories={categories} />
      <SlugField />
    </div>
  );
}

/** Self-contained edit form with its own save action. */
function EditForm({
  product,
  categories,
}: {
  product: AdminProductDetail;
  categories: AdminCategory[];
}) {
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (saving) return;
    setError(null);
    setSaved(false);
    setSaving(true);

    const data = new FormData(e.currentTarget);
    const name = (data.get("name") as string)?.trim();
    const description = (data.get("description") as string)?.trim();
    const priceStr = (data.get("price") as string)?.trim();
    const categoryId = parseInt(data.get("categoryId") as string);
    const slug = (data.get("slug") as string)?.trim();

    if (!name) { setError("El nombre es obligatorio."); setSaving(false); return; }
    const price = parsePrice(priceStr);
    if (price === null) { setError("Ingresá un precio válido, por ejemplo 15000 o 15000,50."); setSaving(false); return; }
    if (price > 99_999_999.99) { setError("El precio máximo es $99.999.999,99."); setSaving(false); return; }
    if (isNaN(categoryId)) { setError("Seleccioná una categoría."); setSaving(false); return; }

    const result = await updateProduct(product.id, {
      name,
      description: description || undefined,
      price,
      categoryId,
      slug: slug || undefined,
    });

    setSaving(false);
    if (result.success) {
      setSaved(true);
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => setSaved(false), 3000);
    } else {
      setError(result.error);
    }
  }

  useEffect(() => () => { if (timeoutRef.current) clearTimeout(timeoutRef.current); }, []);

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-5">
      <NameField defaultValue={product.name} />
      <DescriptionField defaultValue={product.description ?? ""} />
      <PriceField defaultValue={String(product.price)} />
      <CategoryField categories={categories} defaultValue={String(product.category.id)} />
      <SlugField defaultValue={product.slug} hint="Dejar vacío para mantener la URL actual." />

      {error && (
        <p role="alert" className="rounded-soft bg-butter/70 px-4 py-3 text-sm text-danger">
          {error}
        </p>
      )}
      {saved && (
        <p role="status" className="rounded-soft bg-peach/40 px-4 py-3 text-sm">
          Guardado ✓
        </p>
      )}

      <button
        type="submit"
        disabled={saving}
        className="h-12 w-full rounded-full bg-charcoal text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-35 sm:w-auto sm:px-8"
      >
        {saving ? "Guardando…" : "Guardar datos"}
      </button>
    </form>
  );
}

function NameField({ defaultValue = "" }: { defaultValue?: string }) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor="pf-name" className={labelCls}>Nombre *</label>
      <input
        id="pf-name"
        name="name"
        type="text"
        required
        maxLength={150}
        defaultValue={defaultValue}
        className={inputCls}
        placeholder="Conjunto Encaje Floral"
      />
    </div>
  );
}

function DescriptionField({ defaultValue = "" }: { defaultValue?: string }) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor="pf-desc" className={labelCls}>Descripción</label>
      <textarea
        id="pf-desc"
        name="description"
        rows={3}
        maxLength={2000}
        defaultValue={defaultValue}
        className={textareaCls}
        placeholder="Descripción del producto…"
      />
    </div>
  );
}

function PriceField({ defaultValue = "" }: { defaultValue?: string }) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor="pf-price" className={labelCls}>Precio (ARS) *</label>
      <input
        id="pf-price"
        name="price"
        type="text"
        inputMode="decimal"
        required
        defaultValue={defaultValue}
        className={inputCls}
        placeholder="42900"
      />
    </div>
  );
}

function CategoryField({
  categories,
  defaultValue = "",
}: {
  categories: AdminCategory[];
  defaultValue?: string;
}) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor="pf-cat" className={labelCls}>Categoría *</label>
      <select
        id="pf-cat"
        name="categoryId"
        required
        defaultValue={defaultValue}
        className={inputCls}
      >
        <option value="">Seleccioná una categoría</option>
        {categories.map((c) => (
          <option key={c.id} value={c.id}>{c.name}</option>
        ))}
      </select>
    </div>
  );
}

function SlugField({
  defaultValue = "",
  hint,
}: {
  defaultValue?: string;
  hint?: string;
}) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor="pf-slug" className={labelCls}>URL (slug)</label>
      <input
        id="pf-slug"
        name="slug"
        type="text"
        maxLength={170}
        defaultValue={defaultValue}
        className={inputCls}
        placeholder="conjunto-encaje-floral (opcional)"
        pattern="^[a-z0-9]+(-[a-z0-9]+)*$"
        title="Solo letras minúsculas, números y guiones"
      />
      {hint && <p className="text-xs text-charcoal/60">{hint}</p>}
    </div>
  );
}
