import { NextResponse, type NextRequest } from "next/server";
import { ADMIN_COOKIE, ADMIN_LOGIN_PATH as LOGIN_PATH } from "@/lib/admin/cookie";

/**
 * Optimistic /admin guard: only checks that the session cookie exists. The token is
 * validated against the backend by the /admin layout (and by every admin API call).
 */
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasSession = Boolean(request.cookies.get(ADMIN_COOKIE)?.value);
  const isLogin = pathname === LOGIN_PATH || pathname.startsWith(`${LOGIN_PATH}/`);

  if (!hasSession && !isLogin) {
    return NextResponse.redirect(new URL(LOGIN_PATH, request.url));
  }
  if (hasSession && isLogin) {
    return NextResponse.redirect(new URL("/admin", request.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/admin", "/admin/:path*"],
};
