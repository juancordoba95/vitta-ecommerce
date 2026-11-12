import type { Metadata } from "next";
import { OrderConfirmation } from "@/components/checkout/order-confirmation";

export const metadata: Metadata = {
  title: "¡Pedido listo! · Vitta by Marie",
  robots: { index: false },
};

export default function OrderConfirmationPage() {
  return <OrderConfirmation />;
}
