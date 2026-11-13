import "server-only";
import { cookies } from "next/headers";
import { ADMIN_COOKIE } from "./cookie";

export async function getAdminToken(): Promise<string | undefined> {
  return (await cookies()).get(ADMIN_COOKIE)?.value || undefined;
}

/** Stores the token until the JWT's own expiry. Route Handlers / Server Functions only. */
export async function setAdminCookie(token: string, expiresAt: Date): Promise<void> {
  (await cookies()).set(ADMIN_COOKIE, token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    expires: expiresAt,
  });
}

/** Logs out. Route Handlers / Server Functions only. */
export async function clearAdminCookie(): Promise<void> {
  (await cookies()).delete(ADMIN_COOKIE);
}
