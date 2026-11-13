import type { Metadata } from "next";
import Link from "next/link";
import { ADMIN_LINKS } from "@/lib/admin/links";

export const metadata: Metadata = {
  title: "Inicio",
};

export default function AdminHomePage() {
  return (
    <section className="flex flex-col gap-8">
      <div className="flex flex-col gap-3">
        <h1 className="font-script text-5xl">¡Hola!</h1>
        <p className="max-w-prose text-sm leading-relaxed text-charcoal/80">
          Bienvenida al panel de Vitta by Marie. Desde acá vas a poder gestionar el catálogo
          de la tienda.
        </p>
      </div>
      <ul className="grid gap-4 sm:grid-cols-2">
        {ADMIN_LINKS.map(({ href, label }) => (
          <li key={href}>
            <Link
              href={href}
              className="flex h-full items-center justify-between rounded-soft border border-charcoal/10 bg-white/40 px-5 py-6 text-[0.7rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
            >
              {label}
              <span aria-hidden>→</span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
