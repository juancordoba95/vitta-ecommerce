import Link from "next/link";
import { HeartIcon } from "@/components/icons";

export default function ProductNotFound() {
  return (
    <section className="mx-auto flex max-w-sm flex-col items-center gap-4 px-4 py-20 text-center sm:py-28">
      <HeartIcon className="text-charcoal/50" />
      <p className="font-script text-4xl">Ups…</p>
      <h1 className="text-xs leading-relaxed font-normal tracking-label uppercase">
        No encontramos este producto
      </h1>
      <p className="text-sm text-charcoal/70">
        Puede que ya no esté disponible. Mirá todo lo que tenemos para vos.
      </p>
      <Link
        href="/"
        className="mt-4 rounded-full bg-charcoal px-8 py-3.5 text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85"
      >
        Volver al catálogo
      </Link>
    </section>
  );
}
