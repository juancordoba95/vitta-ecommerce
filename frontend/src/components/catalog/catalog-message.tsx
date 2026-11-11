import Link from "next/link";
import { HeartIcon } from "@/components/icons";

type CatalogMessageProps = {
  title: string;
  description?: string;
  /** Shows a "Ver todo" link back to the unfiltered catalog. */
  showAllLink?: boolean;
};

/** Friendly empty / error state for the catalog. */
export function CatalogMessage({
  title,
  description,
  showAllLink = false,
}: CatalogMessageProps) {
  return (
    <div
      role="status"
      className="mx-auto flex max-w-sm flex-col items-center gap-3 py-16 text-center"
    >
      <HeartIcon className="text-charcoal/50" />
      <p className="text-xs leading-relaxed font-normal tracking-label uppercase">
        {title}
      </p>
      {description && (
        <p className="text-sm text-charcoal/70">{description}</p>
      )}
      {showAllLink && (
        <Link
          href="/"
          className="mt-2 border-b border-charcoal/40 pb-0.5 text-[0.7rem] tracking-label uppercase hover:border-charcoal"
        >
          Ver todo
        </Link>
      )}
    </div>
  );
}
