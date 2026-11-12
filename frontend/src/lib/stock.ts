/** A selected size with this many units or fewer shows "Últimas unidades". */
export const LOW_STOCK_THRESHOLD = 3;

export function isLowStock(stock: number): boolean {
  return stock > 0 && stock <= LOW_STOCK_THRESHOLD;
}
