import Link from "next/link";
import type { Category } from "@/lib/api";

type CategoryFilterProps = {
  categories: Category[];
  /** Slug of the selected category; undefined means "Todos". */
  activeSlug?: string;
};

export function CategoryFilter({ categories, activeSlug }: CategoryFilterProps) {
  const options = [
    { label: "Todos", href: "/", active: activeSlug === undefined },
    ...categories.map((category) => ({
      label: category.name,
      href: `/?category=${encodeURIComponent(category.slug)}`,
      active: category.slug === activeSlug,
    })),
  ];

  return (
    <nav aria-label="Categorías" className="-mx-4 overflow-x-auto px-4">
      <ul className="flex w-max gap-2 sm:mx-auto">
        {options.map((option) => (
          <li key={option.href}>
            <Link
              href={option.href}
              scroll={false}
              aria-current={option.active ? "page" : undefined}
              className={`block rounded-full border px-5 py-2.5 text-[0.7rem] tracking-label whitespace-nowrap uppercase transition-colors ${
                option.active
                  ? "border-charcoal bg-charcoal text-cream"
                  : "border-charcoal/20 hover:border-charcoal"
              }`}
            >
              {option.label}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
