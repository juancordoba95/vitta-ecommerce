"use client";

import { useState } from "react";
import type { AdminCategory } from "@/lib/admin/admin-types";
import { createCategory, updateCategory } from "@/app/admin/(panel)/categorias/actions";

const inputCls =
  "h-10 w-full rounded-soft border border-charcoal/20 bg-white/70 px-3 text-sm text-charcoal transition-colors hover:border-charcoal/40 focus:border-charcoal disabled:opacity-60";
const labelCls = "text-[0.6rem] tracking-label uppercase text-charcoal/60";

type Props = {
  initialCategories: AdminCategory[];
};

export function CategoryManager({ initialCategories }: Props) {
  const [categories, setCategories] = useState<AdminCategory[]>(initialCategories);
  const [showCreate, setShowCreate] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  return (
    <div className="flex flex-col gap-5">
      {categories.length === 0 ? (
        <p className="text-sm text-charcoal/60">No hay categorías creadas aún.</p>
      ) : (
        <div className="flex flex-col divide-y divide-charcoal/5 rounded-soft border border-charcoal/10">
          {categories.map((cat) =>
            editingId === cat.id ? (
              <EditCategoryRow
                key={cat.id}
                category={cat}
                onSaved={(updated) => {
                  setCategories((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
                  setEditingId(null);
                }}
                onCancel={() => setEditingId(null)}
              />
            ) : (
              <CategoryRow
                key={cat.id}
                category={cat}
                onEdit={() => setEditingId(cat.id)}
              />
            ),
          )}
        </div>
      )}

      {showCreate ? (
        <CreateCategoryForm
          onCreated={(cat) => {
            setCategories((prev) => [...prev, cat]);
            setShowCreate(false);
          }}
          onCancel={() => setShowCreate(false)}
        />
      ) : (
        <button
          onClick={() => setShowCreate(true)}
          className="self-start rounded-full border border-charcoal/30 px-5 py-2 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal"
        >
          + Nueva categoría
        </button>
      )}
    </div>
  );
}

function CategoryRow({
  category,
  onEdit,
}: {
  category: AdminCategory;
  onEdit: () => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4 bg-white/20 px-4 py-3">
      <div>
        <p className="font-medium text-sm">{category.name}</p>
        <p className="text-xs text-charcoal/50">/{category.slug}</p>
      </div>
      <button
        onClick={onEdit}
        className="text-[0.65rem] tracking-label uppercase text-charcoal/50 underline-offset-2 hover:text-charcoal hover:underline"
      >
        Editar
      </button>
    </div>
  );
}

function EditCategoryRow({
  category,
  onSaved,
  onCancel,
}: {
  category: AdminCategory;
  onSaved: (c: AdminCategory) => void;
  onCancel: () => void;
}) {
  const [name, setName] = useState(category.name);
  const [slug, setSlug] = useState(category.slug);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const result = await updateCategory(category.id, name, slug);
    setSaving(false);
    if (result.success) onSaved(result.data);
    else setError(result.error);
  }

  return (
    <form onSubmit={handleSave} className="flex flex-col gap-3 bg-peach/10 px-4 py-3">
      <div className="flex flex-col gap-1.5">
        <label className={labelCls}>Nombre *</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={100}
          required
          className={inputCls}
          disabled={saving}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <label className={labelCls}>URL (slug)</label>
        <input
          type="text"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          maxLength={120}
          pattern="^[a-z0-9]+(-[a-z0-9]+)*$"
          title="Solo letras minúsculas, números y guiones"
          placeholder="Dejar vacío para mantener la URL actual"
          className={inputCls}
          disabled={saving}
        />
      </div>
      {error && <p className="text-xs text-danger">{error}</p>}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={saving}
          className="h-9 rounded-full bg-charcoal px-5 text-[0.65rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:opacity-35"
        >
          {saving ? "Guardando…" : "Guardar"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="h-9 rounded-full border border-charcoal/20 px-5 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}

function CreateCategoryForm({
  onCreated,
  onCancel,
}: {
  onCreated: (c: AdminCategory) => void;
  onCancel: () => void;
}) {
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const result = await createCategory(name, slug);
    setSaving(false);
    if (result.success) onCreated(result.data);
    else setError(result.error);
  }

  return (
    <form
      onSubmit={handleCreate}
      className="flex flex-col gap-3 rounded-soft border border-charcoal/10 bg-peach/10 px-4 py-4"
    >
      <p className="text-[0.65rem] tracking-label uppercase">Nueva categoría</p>
      <div className="flex flex-col gap-1.5">
        <label className={labelCls}>Nombre *</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={100}
          required
          placeholder="Bodies"
          className={inputCls}
          disabled={saving}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <label className={labelCls}>URL (slug — opcional)</label>
        <input
          type="text"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          maxLength={120}
          pattern="^[a-z0-9]+(-[a-z0-9]+)*$"
          title="Solo letras minúsculas, números y guiones"
          placeholder="bodies (se genera automáticamente)"
          className={inputCls}
          disabled={saving}
        />
      </div>
      {error && <p className="text-xs text-danger">{error}</p>}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={saving}
          className="h-9 rounded-full bg-charcoal px-5 text-[0.65rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:opacity-35"
        >
          {saving ? "Creando…" : "Crear"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="h-9 rounded-full border border-charcoal/20 px-5 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
