import { Suspense, type ReactNode } from "react";
import { AdminAccount, AdminShell } from "@/components/admin/admin-shell";
import { getAdmin, type Admin } from "@/lib/admin/auth";

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
  // Backend failures (ApiError) propagate to app/admin/error.tsx, which offers a retry.
  const admin: Admin = await getAdmin(); // redirects to the login on 401/403

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
