import { BACKEND_TIMEOUT_MS, backendUrl } from "@/lib/api/backend";

/**
 * Same-origin proxy for `POST /api/cart/validate`: the browser validates the cart through
 * here so the backend URL stays server-only. Public and read-only, like the backend one.
 */
export async function POST(request: Request): Promise<Response> {
  const body = await request.text();

  let response: Response;
  try {
    response = await fetch(backendUrl("/api/cart/validate"), {
      method: "POST",
      headers: { Accept: "application/json", "Content-Type": "application/json" },
      body,
      cache: "no-store",
      signal: AbortSignal.timeout(BACKEND_TIMEOUT_MS),
    });
  } catch (error) {
    console.error("[cart] validation proxy failed:", error);
    return Response.json({ error: "unavailable" }, { status: 502 });
  }

  return new Response(await response.text(), {
    status: response.status,
    headers: { "Content-Type": response.headers.get("Content-Type") ?? "application/json" },
  });
}
