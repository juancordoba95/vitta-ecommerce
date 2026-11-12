import Link from "next/link";
import { HeartIcon } from "@/components/icons";
import { StoreChrome } from "@/components/store-chrome";

/** Unknown URLs: shown inside the store frame, like any public page. */
export default function NotFound() {
  return (
    <StoreChrome>
      <section className="mx-auto flex max-w-sm flex-col items-center gap-4 px-4 py-20 text-center sm:py-28">
        <HeartIcon className="text-charcoal/50" />
        <p className="font-script text-4xl">Ups…</p>
        <h1 className="text-xs leading-relaxed font-normal tracking-label uppercase">
          No encontramos esta página
        </h1>
        <Link
          href="/"
          className="mt-4 rounded-full bg-charcoal px-8 py-3.5 text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85"
        >
          Ver catálogo
        </Link>
      </section>
    </StoreChrome>
  );
}
