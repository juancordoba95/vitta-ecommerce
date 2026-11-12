import Link from "next/link";

export function BackToCatalogLink() {
  return (
    <Link
      href="/"
      className="inline-flex items-center gap-2 text-[0.65rem] tracking-label text-charcoal/70 uppercase hover:text-charcoal"
    >
      <span aria-hidden="true">←</span> Volver al catálogo
    </Link>
  );
}
