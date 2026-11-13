import type { Metadata } from "next";
import { LoginForm } from "@/components/admin/login-form";
import { Wordmark } from "@/components/wordmark";

export const metadata: Metadata = {
  title: "Ingresar",
};

export default function AdminLoginPage() {
  return (
    <main className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="flex w-full max-w-sm flex-col gap-8">
        <div className="flex flex-col items-center gap-4 text-center">
          <Wordmark />
          <h1 className="text-xs font-normal tracking-title uppercase">Panel de administración</h1>
        </div>
        <div className="rounded-soft border border-charcoal/10 bg-white/40 p-6 sm:p-8">
          <LoginForm />
        </div>
      </div>
    </main>
  );
}
