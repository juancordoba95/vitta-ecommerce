"use client";

import { HeartIcon } from "@/components/icons";

/**
 * Friendly fallback when the backend does not answer (typically Render's free tier waking
 * up after being idle). `retry` re-fetches and re-renders the failed segment.
 */
export function WakingUp({ retry }: { retry: () => void }) {
  return (
    <section
      role="alert"
      className="mx-auto flex max-w-sm flex-col items-center gap-4 px-4 py-20 text-center sm:py-28"
    >
      <HeartIcon className="text-charcoal/50" />
      <p className="font-script text-4xl">Un momento…</p>
      <h1 className="text-xs leading-relaxed font-normal tracking-label uppercase">
        La tienda está despertando, reintentá en unos segundos
      </h1>
      <button
        type="button"
        onClick={() => retry()}
        className="mt-4 rounded-full bg-charcoal px-8 py-3.5 text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85"
      >
        Reintentar
      </button>
    </section>
  );
}
