"use client";

import { useState } from "react";
import { toggleProductActive } from "@/app/admin/(panel)/productos/actions";

type Props = {
  productId: number;
  initialActive: boolean;
  hasImages: boolean;
  hasStock: boolean;
};

export function PublishSection({ productId, initialActive, hasImages, hasStock }: Props) {
  const [active, setActive] = useState(initialActive);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const warnings: string[] = [];
  if (!active) {
    if (!hasImages) warnings.push("El producto no tiene fotos.");
    if (!hasStock) warnings.push("El producto no tiene stock disponible.");
  }

  async function handleToggle() {
    setLoading(true);
    setError(null);
    const result = await toggleProductActive(productId, !active);
    setLoading(false);
    if (result.success) setActive(result.data.active);
    else setError(result.error);
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-4">
        <span
          className={`inline-flex items-center rounded-full px-3 py-1 text-[0.65rem] tracking-label uppercase ${
            active
              ? "bg-peach/50 text-charcoal"
              : "bg-charcoal/10 text-charcoal/60"
          }`}
        >
          {active ? "Publicado" : "Oculto"}
        </span>
        <button
          onClick={handleToggle}
          disabled={loading}
          className={`h-10 rounded-full px-6 text-[0.7rem] tracking-label uppercase transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-35 ${
            active
              ? "bg-charcoal/10 text-charcoal hover:bg-charcoal/20"
              : "bg-charcoal text-cream"
          }`}
        >
          {loading ? "…" : active ? "Ocultar" : "Publicar"}
        </button>
      </div>

      {warnings.length > 0 && !active && (
        <div className="rounded-soft border border-butter bg-butter/40 px-4 py-3 text-sm">
          <p className="mb-1 font-medium">Aviso antes de publicar:</p>
          <ul className="list-inside list-disc space-y-0.5 text-charcoal/80">
            {warnings.map((w) => <li key={w}>{w}</li>)}
          </ul>
          <p className="mt-2 text-xs text-charcoal/60">
            Podés publicar igual, pero los clientes no van a poder agregarlo al carrito.
          </p>
        </div>
      )}

      {error && <p role="alert" className="text-sm text-danger">{error}</p>}
    </div>
  );
}
