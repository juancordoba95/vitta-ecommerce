/** Response types of the Vitta backend API (see docs/spec.md, "API"). */

export type Category = {
  id: number;
  name: string;
  slug: string;
};

export type ProductSummary = {
  id: number;
  name: string;
  slug: string;
  /** Price in ARS. */
  price: number;
  imageUrl: string | null;
  categorySlug: string;
};

export type ProductVariant = {
  size: string;
  stock: number;
};

export type ProductDetail = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  /** Price in ARS. */
  price: number;
  imageUrl: string | null;
  category: Category;
  variants: ProductVariant[];
};

/** RFC 9457 error body returned by the backend. */
export type ProblemDetail = {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
};
