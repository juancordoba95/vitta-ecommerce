import type { ProductSummary } from "@/lib/api";
import { ProductCard } from "./product-card";

export function ProductGrid({ products }: { products: ProductSummary[] }) {
  return (
    <ul className="grid grid-cols-2 gap-x-4 gap-y-10 md:grid-cols-3 lg:grid-cols-4 lg:gap-x-6">
      {products.map((product) => (
        <li key={product.id}>
          <ProductCard product={product} />
        </li>
      ))}
    </ul>
  );
}
