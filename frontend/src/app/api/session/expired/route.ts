import { ADMIN_LOGIN_PATH } from "@/lib/admin/cookie";
import { clearAdminCookie } from "@/lib/admin/session";

/**
 * Target of the /admin layout when the backend rejects the token (expired or invalid).
 * Server Components can't delete cookies, and the proxy would bounce /admin/login back
 * to /admin while the stale cookie exists, so it is cleared here before the redirect.
 */
export async function GET(request: Request): Promise<Response> {
  await clearAdminCookie();
  return Response.redirect(new URL(ADMIN_LOGIN_PATH, request.url), 303);
}
