import { backendUrl } from "@/lib/admin/backend";
import { getAdminToken } from "@/lib/admin/session";
import type { NextRequest } from "next/server";

/**
 * Proxies image uploads to the backend with the admin JWT from the session cookie.
 * Accepts a single `file` field; wraps it in `files` for the backend.
 * Returns the backend's gallery response or an error object.
 */
export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ productId: string }> },
): Promise<Response> {
  const token = await getAdminToken();
  if (!token) {
    return Response.json({ error: "Sin sesión activa" }, { status: 401 });
  }

  const { productId } = await params;
  if (!productId || isNaN(Number(productId))) {
    return Response.json({ error: "ID de producto inválido" }, { status: 400 });
  }

  let formData: FormData;
  try {
    formData = await request.formData();
  } catch {
    return Response.json({ error: "No se pudo leer el archivo" }, { status: 400 });
  }

  const file = formData.get("file");
  if (!(file instanceof File)) {
    return Response.json({ error: "Falta el archivo" }, { status: 400 });
  }

  const backendForm = new FormData();
  backendForm.append("files", file, file.name || "image.jpg");

  let response: Response;
  try {
    response = await fetch(backendUrl(`/api/admin/products/${productId}/images`), {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: backendForm,
    });
  } catch (cause) {
    console.error("[admin] image upload proxy failed:", cause);
    return Response.json({ error: "No se pudo conectar con el servidor" }, { status: 502 });
  }

  if (response.status === 401 || response.status === 403) {
    return Response.json({ error: "Sesión expirada" }, { status: 401 });
  }

  const body = await response.json().catch(() => null);
  return Response.json(body, { status: response.status });
}
