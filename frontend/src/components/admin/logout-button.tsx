"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

/** Clears the session cookie (`DELETE /api/session`) and goes back to the login. */
export function LogoutButton({ className = "" }: { className?: string }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);

  async function handleClick() {
    setPending(true);
    try {
      const response = await fetch("/api/session", { method: "DELETE" });
      if (!response.ok) throw new Error(`DELETE /api/session failed with ${response.status}`);
    } catch (error) {
      console.warn("[admin] logout failed:", error);
      setPending(false);
      return;
    }
    router.replace("/admin/login");
    router.refresh();
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={pending}
      className={`rounded-full border border-charcoal/30 px-5 py-2 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal disabled:opacity-50 ${className}`}
    >
      {pending ? "Saliendo…" : "Cerrar sesión"}
    </button>
  );
}
