const wholePesos = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 0,
});

const pesosWithCents = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  minimumFractionDigits: 2,
});

/** Formats a price in Argentine pesos: 42900 → "$ 42.900", 42900.5 → "$ 42.900,50". */
export function formatPrice(amount: number): string {
  return Number.isInteger(amount)
    ? wholePesos.format(amount)
    : pesosWithCents.format(amount);
}
