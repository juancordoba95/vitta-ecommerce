"use client";

import Image from "next/image";
import { useRef, useState } from "react";
import type { AdminImage } from "@/lib/admin/admin-types";
import { deleteImage, reorderImages } from "@/app/admin/(panel)/productos/actions";

type UploadStatus = { name: string; status: "uploading" | "done" | "error"; error?: string };

type Props = {
  productId: number;
  initialImages: AdminImage[];
};

/** Resizes an image file client-side to at most maxDim px on the longest side, returning a JPEG blob. */
function resizeToJpeg(file: File, maxDim: number, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const img = new window.Image();
    const objectUrl = URL.createObjectURL(file);
    img.onload = () => {
      URL.revokeObjectURL(objectUrl);
      let w = img.naturalWidth;
      let h = img.naturalHeight;
      if (w > maxDim || h > maxDim) {
        if (w >= h) { h = Math.round((h * maxDim) / w); w = maxDim; }
        else { w = Math.round((w * maxDim) / h); h = maxDim; }
      }
      const canvas = document.createElement("canvas");
      canvas.width = w;
      canvas.height = h;
      canvas.getContext("2d")!.drawImage(img, 0, 0, w, h);
      canvas.toBlob(
        (blob) => (blob ? resolve(blob) : reject(new Error("Canvas toBlob returned null"))),
        "image/jpeg",
        quality,
      );
    };
    img.onerror = () => { URL.revokeObjectURL(objectUrl); reject(new Error("Could not load image")); };
    img.src = objectUrl;
  });
}

function uploadErrorMessage(status: number): string {
  if (status === 401) return "Sesión expirada.";
  if (status === 409) return "El producto ya tiene el máximo de fotos (8).";
  if (status === 413) return "La imagen supera los 5 MB.";
  if (status === 400) return "Formato de imagen no válido (aceptamos JPEG, PNG o WebP).";
  return "Error al subir la imagen.";
}

export function ImageManager({ productId, initialImages }: Props) {
  const [images, setImages] = useState<AdminImage[]>(initialImages);
  const [uploads, setUploads] = useState<UploadStatus[]>([]);
  const [actionError, setActionError] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const uploading = uploads.some((u) => u.status === "uploading");

  async function handleFiles(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length === 0) return;
    if (fileRef.current) fileRef.current.value = "";

    const statuses: UploadStatus[] = files.map((f) => ({ name: f.name, status: "uploading" }));
    setUploads(statuses);
    setActionError(null);

    for (let i = 0; i < files.length; i++) {
      let blob: Blob;
      try {
        blob = await resizeToJpeg(files[i], 1600, 0.85);
      } catch {
        setUploads((prev) =>
          prev.map((u, idx) => idx === i ? { ...u, status: "error", error: "No se pudo procesar la imagen." } : u),
        );
        continue;
      }

      const form = new FormData();
      form.append("file", blob, files[i].name.replace(/\.[^.]+$/, ".jpg") || "image.jpg");

      let res: Response;
      try {
        res = await fetch(`/api/admin/images/${productId}`, { method: "POST", body: form });
      } catch {
        setUploads((prev) =>
          prev.map((u, idx) => idx === i ? { ...u, status: "error", error: "Error de red." } : u),
        );
        continue;
      }

      if (!res.ok) {
        const errMsg = uploadErrorMessage(res.status);
        setUploads((prev) =>
          prev.map((u, idx) => idx === i ? { ...u, status: "error", error: errMsg } : u),
        );
        continue;
      }

      const gallery = (await res.json()) as AdminImage[];
      setImages(gallery);
      setUploads((prev) => prev.map((u, idx) => idx === i ? { ...u, status: "done" } : u));
    }
  }

  async function handleMove(imageId: number, direction: "up" | "down") {
    setActionError(null);
    const idx = images.findIndex((img) => img.id === imageId);
    if (idx < 0) return;
    const newImages = [...images];
    const swapIdx = direction === "up" ? idx - 1 : idx + 1;
    if (swapIdx < 0 || swapIdx >= newImages.length) return;
    [newImages[idx], newImages[swapIdx]] = [newImages[swapIdx], newImages[idx]];
    const ids = newImages.map((img) => img.id);
    // Optimistic update
    setImages(newImages.map((img, i) => ({ ...img, position: i })));
    const result = await reorderImages(productId, ids);
    if (result.success) setImages(result.data);
    else { setActionError(result.error); setImages(images); }
  }

  async function handleDelete() {
    if (confirmDeleteId === null) return;
    setDeleting(true);
    setActionError(null);
    const result = await deleteImage(confirmDeleteId);
    setDeleting(false);
    if (result.success) {
      setImages((prev) => {
        const updated = prev.filter((img) => img.id !== confirmDeleteId);
        return updated.map((img, i) => ({ ...img, position: i }));
      });
      setConfirmDeleteId(null);
    } else {
      setActionError(result.error);
      setConfirmDeleteId(null);
    }
  }

  return (
    <div className="flex flex-col gap-5">
      {images.length === 0 ? (
        <p className="text-sm text-charcoal/60">No hay fotos cargadas aún.</p>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
          {images.map((img, idx) => (
            <div
              key={img.id}
              className={`relative flex flex-col overflow-hidden rounded-soft border ${
                idx === 0 ? "border-charcoal/40" : "border-charcoal/10"
              } bg-white/40`}
            >
              <div className="relative aspect-square w-full">
                <Image
                  src={img.url}
                  alt={`Foto ${idx + 1}`}
                  fill
                  className="object-cover"
                  sizes="(max-width: 640px) 50vw, (max-width: 768px) 33vw, 25vw"
                  unoptimized={img.url.includes("placehold.co")}
                />
              </div>
              {idx === 0 && (
                <span className="absolute left-1 top-1 rounded-full bg-charcoal/80 px-2 py-0.5 text-[0.55rem] tracking-label text-cream uppercase">
                  Portada
                </span>
              )}
              <div className="flex items-center justify-between gap-1 px-2 py-1.5">
                <div className="flex gap-1">
                  <button
                    onClick={() => handleMove(img.id, "up")}
                    disabled={idx === 0 || uploading}
                    title="Mover arriba"
                    className="flex h-7 w-7 items-center justify-center rounded border border-charcoal/15 text-sm transition-colors hover:border-charcoal/40 disabled:opacity-30"
                  >
                    ↑
                  </button>
                  <button
                    onClick={() => handleMove(img.id, "down")}
                    disabled={idx === images.length - 1 || uploading}
                    title="Mover abajo"
                    className="flex h-7 w-7 items-center justify-center rounded border border-charcoal/15 text-sm transition-colors hover:border-charcoal/40 disabled:opacity-30"
                  >
                    ↓
                  </button>
                </div>
                <button
                  onClick={() => setConfirmDeleteId(img.id)}
                  disabled={uploading}
                  title="Eliminar foto"
                  className="text-xs text-charcoal/40 transition-colors hover:text-danger disabled:opacity-30"
                >
                  ×
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Confirm delete dialog */}
      {confirmDeleteId !== null && (
        <div className="flex items-center gap-3 rounded-soft border border-charcoal/10 bg-butter/40 px-4 py-3">
          <p className="flex-1 text-sm">¿Eliminar esta foto?</p>
          <button
            onClick={handleDelete}
            disabled={deleting}
            className="h-8 rounded-full bg-danger px-4 text-[0.65rem] tracking-label text-cream uppercase transition-opacity hover:opacity-85 disabled:opacity-50"
          >
            {deleting ? "…" : "Eliminar"}
          </button>
          <button
            onClick={() => setConfirmDeleteId(null)}
            className="h-8 rounded-full border border-charcoal/20 px-4 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal/40"
          >
            Cancelar
          </button>
        </div>
      )}

      {actionError && (
        <p role="alert" className="text-sm text-danger">{actionError}</p>
      )}

      {/* Upload progress */}
      {uploads.length > 0 && (
        <ul className="flex flex-col gap-1">
          {uploads.map((u, i) => (
            <li key={i} className="flex items-center gap-2 text-sm">
              <span className={u.status === "error" ? "text-danger" : "text-charcoal/70"}>
                {u.status === "uploading" ? "⏳" : u.status === "done" ? "✓" : "✗"}
              </span>
              <span className="truncate">{u.name}</span>
              {u.error && <span className="text-xs text-danger">{u.error}</span>}
            </li>
          ))}
        </ul>
      )}

      <div>
        <input
          ref={fileRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          className="sr-only"
          id="image-upload-input"
          onChange={handleFiles}
          disabled={uploading}
        />
        <label
          htmlFor="image-upload-input"
          className={`inline-flex h-10 cursor-pointer items-center rounded-full border border-charcoal/30 px-5 text-[0.65rem] tracking-label uppercase transition-colors hover:border-charcoal ${
            uploading ? "cursor-not-allowed opacity-40" : ""
          }`}
        >
          {uploading ? "Subiendo…" : "+ Subir fotos"}
        </label>
        <p className="mt-1.5 text-xs text-charcoal/50">
          JPEG, PNG o WebP · máx. 5 MB por foto · máx. 8 fotos
        </p>
      </div>
    </div>
  );
}
