"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ADMIN_LINKS } from "@/lib/admin/links";

/** Section links, with the current one highlighted. */
export function AdminNavLinks() {
  const pathname = usePathname();
  return (
    <ul className="flex gap-2 lg:flex-col lg:gap-1">
      {ADMIN_LINKS.map(({ href, label }) => {
        const active = pathname === href || pathname.startsWith(`${href}/`);
        return (
          <li key={href}>
            <Link
              href={href}
              aria-current={active ? "page" : undefined}
              className={`block rounded-full px-4 py-2 text-[0.7rem] tracking-label whitespace-nowrap uppercase transition-colors lg:rounded-soft ${
                active ? "bg-peach/60" : "hover:bg-peach/30"
              }`}
            >
              {label}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
