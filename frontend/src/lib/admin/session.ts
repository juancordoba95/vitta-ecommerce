import "server-only";
import { cookies } from "next/headers";
import { ADMIN_COOKIE } from "./cookie";

/**
 * Attributes shared by set and clear: a cookie is only overwritten or removed when name,
 * path (and secure) match. `secure` only in production so plain-http localhost still works.
 */
function cookieAttributes() {
  return {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
  } as const;
}

export async function getAdminToken(): Promise<string | undefined> {
  return (await cookies()).get(ADMIN_COOKIE)?.value || undefined;
}

/** Stores the token until the JWT's own expiry. Route Handlers / Server Functions only. */
export async function setAdminCookie(token: string, expiresAt: Date): Promise<void> {
  (await cookies()).set(ADMIN_COOKIE, token, { ...cookieAttributes(), expires: expiresAt });
}

/** Logs out (expires the cookie with the same attributes). Route Handlers / Server Functions only. */
export async function clearAdminCookie(): Promise<void> {
  (await cookies()).set(ADMIN_COOKIE, "", { ...cookieAttributes(), maxAge: 0 });
}
