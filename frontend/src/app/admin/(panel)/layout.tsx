import { Suspense, type ReactNode } from "react";
import { AdminAccount, AdminShell } from "@/components/admin/admin-shell";
import { getAdmin, type Admin } from "@/lib/admin/auth";
import { ApiError } from "@/lib/api/client";

/**
 * Protected admin section. The proxy already sent visitors without a cookie to the
 * login; here the token is validated against the backend before rendering anything.
 */
export default function AdminPanelLayout({ children }: LayoutProps<"/admin">) {
  // The session is request data: it streams in behind the boundary (cacheComponents).
  return (
    <Suspense fallback={<PanelSkeleton />}>
      <AdminSession>{children}</AdminSession>
    </Suspense>
  );
}

async function AdminSession({ children }: { children: ReactNode }) {
  let admin: Admin;
  try {
    admin = await getAdmin(); // redirects to the login on 401/403
  } catch (error) {
    if (!(error instanceof ApiError)) throw error;
    console.error("[admin] could not validate the session:", error);
    return <BackendUnavailable />;
  }

  return <AdminShell account={<AdminAccount email={admin.email} />}>{children}</AdminShell>;
}

function PanelSkeleton() {
  return (
    <AdminShell account={<div aria-hidden className="h-9 animate-pulse rounded-full bg-peach/30" />}>
      <div aria-busy="true" aria-label="Cargando el panel" className="flex animate-pulse flex-col gap-4">
        <div className="h-10 w-2/3 max-w-sm rounded-soft bg-peach/30" />
        <div className="h-24 rounded-soft bg-peach/30" />
      </div>
    </AdminShell>
  );
}

function BackendUnavailable() {
  return (
    <main className="flex flex-1 items-center justify-center px-4 py-20">
      <div className="flex max-w-sm flex-col items-center gap-4 text-center">
        <h1 className="text-xs font-normal tracking-label uppercase">
          No pudimos conectar con el servidor
        </h1>
        <p className="text-sm text-charcoal/70">Revisá tu conexión y recargá la página.</p>
      </div>
    </main>
  );
}
