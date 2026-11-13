"use client";

import { useState } from "react";
import type { AdminVariant } from "@/lib/admin/admin-types";
import {
  addVariant,
  deleteVariant,
  updateVariantStock,
} from "@/app/admin/(panel)/productos/actions";

const inputCls =
  "h-10 rounded-soft border border-charcoal/20 bg-white/70 px-3 text-sm text-charcoal transition-colors hover:border-charcoal/40 focus:border-charcoal disabled:opacity-60";

type Props = {
  productId: number;
  initialVariants: AdminVariant[];
};

export function VariantManager({ productId, initialVariants }: Props) {
  const [variants, setVariants] = useState<AdminVariant[]>(initialVariants);

  return (
    <div className="flex flex-col gap-4">
      {variants.length === 0 ? (
        <p className="text-sm text-charcoal/60">No hay talles cargados aún.</p>
      ) : (
        <div className="overflow-x-auto rounded-soft border border-charcoal/10">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-charcoal/10 bg-cream/60">
                <th className="px-4 py-3 text-left text-[0.65rem] tracking-label uppercase font-normal">
                  Talle
                </th>
                <th className="px-4 py-3 text-left text-[0.65rem] tracking-label uppercase font-normal">
                  Stock
                </th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-charcoal/5">
              {variants.map((v) => (
                <VariantRow
                  key={v.id}
                  variant={v}
                  onUpdated={(updated) =>
                    setVariants((prev) => prev.map((x) => (x.id === updated.id ? updated : x)))
                  }
                  onDeleted={() =>
                    setVariants((prev) => prev.filter((x) => x.id !== v.id))
                  }
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
      <AddVariantForm
        productId={productId}
        onAdded={(v) => setVariants((prev) => [...prev, v])}
      />
    </div>
  );
}

function VariantRow({
  variant,
  onUpdated,
  onDeleted,
}: {
  variant: AdminVariant;
  onUpdated: (v: AdminVariant) => void;
  onDeleted: () => void;
}) {
  const [stock, setStock] = useState(String(variant.stock));
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const stockChanged = stock !== String(variant.stock);

  async function handleSaveStock() {
    const val = parseInt(stock);
    if (isNaN(val) || val < 0) { setError("Stock inválido."); return; }
    setSaving(true);
    setError(null);
    const result = await updateVariantStock(variant.id, val);
    setSaving(false);
    if (result.success) onUpdated(result.data);
    else setError(result.error);
  }

  async function handleDelete() {
    setDeleting(true);
    const result = await deleteVariant(variant.id);
    setDeleting(false);
    if (result.success) onDeleted();
    else { setError(result.error); setConfirmDelete(false); }
  }

  return (
    <tr className="bg-white/20">
      <td className="px-4 py-3 font-medium">{variant.size}</td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <input
            type="number"
            min={0}
            max={9999}
            value={stock}
            onChange={(e) => setStock(e.target.value)}
            className={`${inputCls} w-24`}
            disabled={saving}
          />
          {stockChanged && (
            <button
              onClick={handleSaveStock}
              disabled={saving}
              className="h-10 rounded-full bg-charcoal px-4 text-[0.65rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:opacity-35"
            >
              {saving ? "…" : "Guardar"}
            </button>
          )}
        </div>
        {error && <p className="mt-1 text-xs text-danger">{error}</p>}
      </td>
      <td className="px-4 py-3 text-right">
        {confirmDelete ? (
          <span className="flex items-center justify-end gap-2">
            <span className="text-xs text-charcoal/70">¿Eliminar?</span>
            <button
              onClick={handleDelete}
              disabled={deleting}
              className="h-8 rounded-full bg-danger px-3 text-[0.6rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:opacity-50"
            >
              {deleting ? "…" : "Sí"}
            </button>
            <button
              onClick={() => setConfirmDelete(false)}
              className="h-8 rounded-full border border-charcoal/20 px-3 text-[0.6rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
            >
              No
            </button>
          </span>
        ) : (
          <button
            onClick={() => setConfirmDelete(true)}
            className="text-xs text-charcoal/50 underline-offset-2 hover:text-danger hover:underline"
          >
            Eliminar
          </button>
        )}
      </td>
    </tr>
  );
}

function AddVariantForm({
  productId,
  onAdded,
}: {
  productId: number;
  onAdded: (v: AdminVariant) => void;
}) {
  const [size, setSize] = useState("");
  const [stock, setStock] = useState("0");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    if (!size.trim()) { setError("El talle es obligatorio."); return; }
    const stockVal = parseInt(stock);
    if (isNaN(stockVal) || stockVal < 0) { setError("Stock inválido."); return; }
    setSaving(true);
    setError(null);
    const result = await addVariant(productId, size.trim().toUpperCase(), stockVal);
    setSaving(false);
    if (result.success) {
      onAdded(result.data);
      setSize("");
      setStock("0");
    } else {
      setError(result.error);
    }
  }

  return (
    <form onSubmit={handleAdd} className="flex flex-col gap-3">
      <p className="text-[0.65rem] tracking-label uppercase text-charcoal/70">Agregar talle</p>
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1">
          <label className="text-xs text-charcoal/60">Talle</label>
          <input
            type="text"
            maxLength={10}
            value={size}
            onChange={(e) => setSize(e.target.value)}
            placeholder="S, M, L, XL…"
            className={`${inputCls} w-28`}
            disabled={saving}
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs text-charcoal/60">Stock</label>
          <input
            type="number"
            min={0}
            max={9999}
            value={stock}
            onChange={(e) => setStock(e.target.value)}
            className={`${inputCls} w-24`}
            disabled={saving}
          />
        </div>
        <button
          type="submit"
          disabled={saving}
          className="h-10 rounded-full border border-charcoal/30 px-5 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal disabled:opacity-35"
        >
          {saving ? "Agregando…" : "+ Agregar"}
        </button>
      </div>
      {error && <p className="text-xs text-danger">{error}</p>}
    </form>
  );
}
