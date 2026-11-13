import { BACKEND_TIMEOUT_MS, backendUrl } from "@/lib/admin/backend";
import { clearAdminCookie, setAdminCookie } from "@/lib/admin/session";

/** What the browser gets back: never the token itself. */
type SessionResponse = { ok: true } | { error: "invalid" | "rate_limited" | "unavailable" };

type LoginResponse = { token: string; expiresAt: string };

function json(body: SessionResponse, status: number, headers?: HeadersInit): Response {
  return Response.json(body, { status, headers });
}

function isLoginResponse(value: unknown): value is LoginResponse {
  const candidate = value as Partial<LoginResponse> | null;
  return (
    typeof candidate?.token === "string" &&
    candidate.token.length > 0 &&
    typeof candidate.expiresAt === "string" &&
    !Number.isNaN(Date.parse(candidate.expiresAt))
  );
}

/**
 * Admin login: forwards the credentials to the backend from the Next server and keeps
 * the JWT in the httpOnly `vitta_admin` cookie until it expires.
 */
export async function POST(request: Request): Promise<Response> {
  const body = (await request.json().catch(() => null)) as {
    email?: unknown;
    password?: unknown;
  } | null;
  const email = typeof body?.email === "string" ? body.email.trim() : "";
  const password = typeof body?.password === "string" ? body.password : "";
  if (!email || !password) return json({ error: "invalid" }, 400);

  let response: Response;
  try {
    response = await fetch(backendUrl("/api/auth/login"), {
      method: "POST",
      headers: { Accept: "application/json", "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
      cache: "no-store",
      signal: AbortSignal.timeout(BACKEND_TIMEOUT_MS),
    });
  } catch (error) {
    console.error("[admin] login request failed:", error);
    return json({ error: "unavailable" }, 502);
  }

  if (response.status === 400 || response.status === 401) {
    return json({ error: "invalid" }, response.status);
  }
  if (response.status === 429) {
    const retryAfter = response.headers.get("Retry-After");
    return json({ error: "rate_limited" }, 429, retryAfter ? { "Retry-After": retryAfter } : undefined);
  }

  const login: unknown = response.ok ? await response.json().catch(() => null) : null;
  if (!isLoginResponse(login)) {
    console.error(`[admin] unexpected login response (${response.status})`);
    return json({ error: "unavailable" }, 502);
  }

  await setAdminCookie(login.token, new Date(login.expiresAt));
  return json({ ok: true }, 200);
}

/** Logout: drops the session cookie. The JWT itself just expires on its own. */
export async function DELETE(): Promise<Response> {
  await clearAdminCookie();
  return new Response(null, { status: 204 });
}
