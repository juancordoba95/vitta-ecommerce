import Link from "next/link";
import { Suspense, type ReactNode } from "react";
import { Wordmark } from "@/components/wordmark";
import { ADMIN_LINKS } from "@/lib/admin/links";
import { AdminNavLinks } from "./admin-nav-links";
import { LogoutButton } from "./logout-button";

/**
 * Admin frame: a top bar on phones, a sidebar from `lg`. `account` (the admin's email
 * and logout) is passed in by the layout once the session is validated.
 */
export function AdminShell({ account, children }: { account: ReactNode; children: ReactNode }) {
  return (
    <div className="flex flex-1 flex-col lg:flex-row">
      <aside className="border-b border-charcoal/10 lg:sticky lg:top-0 lg:flex lg:h-dvh lg:w-64 lg:shrink-0 lg:flex-col lg:border-r lg:border-b-0">
        <div className="flex items-center justify-between gap-4 px-4 py-4 lg:flex-col lg:items-start lg:gap-3 lg:px-6 lg:py-8">
          <Link href="/admin" aria-label="Vitta by Marie, inicio del panel">
            <Wordmark />
          </Link>
          <span className="rounded-full bg-vitta-gradient px-3 py-1 text-[0.6rem] tracking-label uppercase">
            Admin
          </span>
        </div>
        <nav aria-label="Panel" className="overflow-x-auto px-2 pb-3 lg:flex-1 lg:px-4">
          {/* Suspense prevents usePathname() from blocking prerendering of dynamic routes. */}
          <Suspense fallback={<NavFallback />}>
            <AdminNavLinks />
          </Suspense>
        </nav>
        <div className="border-t border-charcoal/10 px-4 py-3 lg:px-6 lg:py-6">{account}</div>
      </aside>
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8 sm:px-8 lg:py-12">
        {children}
      </main>
    </div>
  );
}

/** Static nav links without active-page highlighting — used as Suspense fallback. */
function NavFallback() {
  return (
    <ul className="flex gap-2 lg:flex-col lg:gap-1">
      {ADMIN_LINKS.map(({ href, label }) => (
        <li key={href}>
          <Link
            href={href}
            className="block rounded-full px-4 py-2 text-[0.7rem] tracking-label whitespace-nowrap uppercase transition-colors hover:bg-peach/30 lg:rounded-soft"
          >
            {label}
          </Link>
        </li>
      ))}
    </ul>
  );
}

/** Admin email plus logout. */
export function AdminAccount({ email }: { email: string }) {
  return (
    <div className="flex items-center justify-between gap-3 lg:flex-col lg:items-start">
      <p className="min-w-0 truncate text-xs text-charcoal/70" title={email}>
        {email}
      </p>
      <LogoutButton className="shrink-0" />
    </div>
  );
}
