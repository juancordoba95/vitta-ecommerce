/**
 * Parses a price string written in es-AR or standard format.
 *
 * Accepted formats:
 *   15000        → 15000
 *   15.000       → 15000  (dot as thousands separator)
 *   15000,50     → 15000.5
 *   15.000,50    → 15000.5
 *   15000.50     → 15000.5 (dot as decimal)
 *
 * Returns null for empty, non-numeric, zero, or negative values.
 */
export function parsePrice(raw: string): number | null {
  const s = raw.trim();
  if (!s) return null;

  let normalized: string;

  if (s.includes(",")) {
    // Comma present → comma is decimal separator, dots are thousands separators
    normalized = s.replace(/\./g, "").replace(",", ".");
  } else {
    const dotCount = (s.match(/\./g) ?? []).length;
    if (dotCount > 1) {
      // Multiple dots → all are thousands separators (e.g. 1.000.000)
      normalized = s.replace(/\./g, "");
    } else if (dotCount === 1) {
      const afterDot = s.split(".")[1];
      if (afterDot.length === 3) {
        // Single dot with exactly 3 digits after → thousands separator (15.000)
        normalized = s.replace(".", "");
      } else {
        // Decimal separator (15000.50)
        normalized = s;
      }
    } else {
      normalized = s;
    }
  }

  const n = parseFloat(normalized);
  if (!isFinite(n) || n <= 0) return null;
  return n;
}
