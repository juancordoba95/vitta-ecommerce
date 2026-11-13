/** Admin-specific API response types (see docs/spec.md, "Admin catalog"). */

export type AdminCategory = {
  id: number;
  name: string;
  slug: string;
};

export type AdminProductSummary = {
  id: number;
  name: string;
  slug: string;
  price: number;
  imageUrl: string | null;
  category: AdminCategory;
  active: boolean;
  updatedAt: string;
};

export type AdminProductPage = {
  content: AdminProductSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminVariant = {
  id: number;
  size: string;
  stock: number;
};

export type AdminImage = {
  id: number;
  url: string;
  position: number;
};

export type AdminProductDetail = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  active: boolean;
  category: AdminCategory;
  images: AdminImage[];
  variants: AdminVariant[];
  createdAt: string;
  updatedAt: string;
};
