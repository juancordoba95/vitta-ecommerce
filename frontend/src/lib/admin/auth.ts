import "server-only";
import { redirect } from "next/navigation";
import { AdminUnauthorizedError, adminFetch } from "./admin-fetch";

export type Admin = { email: string };

/** Clears the stale cookie and sends the admin to the login (see the route handler). */
export const SESSION_EXPIRED_PATH = "/api/session/expired";

/**
 * The logged-in admin, validated against the backend (`GET /api/admin/me`). An
 * expired or invalid token redirects to the login after clearing the cookie; other
 * failures (backend down) throw `ApiError`.
 */
export async function getAdmin(): Promise<Admin> {
  try {
    const { email } = await adminFetch<Admin>("/api/admin/me");
    return { email };
  } catch (error) {
    if (error instanceof AdminUnauthorizedError) redirect(SESSION_EXPIRED_PATH);
    throw error;
  }
}
