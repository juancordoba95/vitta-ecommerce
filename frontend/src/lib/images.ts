/**
 * placehold.co serves SVG, which the Image Optimization API rejects by default.
 * Next.js recommends `unoptimized` for SVG sources, so only these skip optimization.
 */
export function isPlaceholderImage(url: string): boolean {
  try {
    return new URL(url).hostname === "placehold.co";
  } catch {
    return false;
  }
}
