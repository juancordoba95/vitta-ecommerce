"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";

const ERRORS = {
  invalid: "Email o contraseña incorrectos",
  rateLimited: "Demasiados intentos, probá de nuevo en un minuto",
  generic: "No pudimos iniciar sesión. Probá de nuevo en unos minutos.",
} as const;

type LoginError = keyof typeof ERRORS;

const labelClass = "text-[0.65rem] font-normal tracking-label uppercase";
const inputClass =
  "h-12 w-full rounded-soft border border-charcoal/20 bg-white/70 px-4 text-base text-charcoal transition-colors placeholder:text-charcoal/40 hover:border-charcoal/40 focus:border-charcoal disabled:opacity-60 sm:text-sm";

function toError(status: number): LoginError {
  if (status === 401 || status === 400) return "invalid";
  if (status === 429) return "rateLimited";
  return "generic";
}

/** Admin login. The session cookie is set by `POST /api/session`; no token reaches JS. */
export function LoginForm() {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<LoginError | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    const data = new FormData(event.currentTarget);
    setError(null);
    setSubmitting(true);

    let response: Response;
    try {
      response = await fetch("/api/session", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: data.get("email"), password: data.get("password") }),
      });
    } catch {
      setError("generic");
      setSubmitting(false);
      return;
    }

    if (!response.ok) {
      setError(toError(response.status));
      setSubmitting(false);
      return;
    }
    // The button stays disabled while the panel loads.
    router.replace("/admin");
    router.refresh();
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <label htmlFor="login-email" className={labelClass}>
          Email
        </label>
        <input
          id="login-email"
          name="email"
          type="email"
          required
          maxLength={254}
          autoComplete="username"
          disabled={submitting}
          className={inputClass}
        />
      </div>
      <div className="flex flex-col gap-2">
        <label htmlFor="login-password" className={labelClass}>
          Contraseña
        </label>
        <input
          id="login-password"
          name="password"
          type="password"
          required
          maxLength={72}
          autoComplete="current-password"
          disabled={submitting}
          className={inputClass}
        />
      </div>
      {error && (
        <p role="alert" className="rounded-soft bg-butter/70 px-4 py-3 text-sm">
          {ERRORS[error]}
        </p>
      )}
      <button
        type="submit"
        disabled={submitting}
        className="h-12 w-full rounded-full bg-charcoal text-[0.7rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-35"
      >
        {submitting ? "Ingresando…" : "Ingresar"}
      </button>
    </form>
  );
}
