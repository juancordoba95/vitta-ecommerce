import type { Metadata } from "next";
import { ComingSoon } from "@/components/admin/coming-soon";

export const metadata: Metadata = {
  title: "Productos",
};

export default function AdminProductsPage() {
  return <ComingSoon title="Productos" />;
}
