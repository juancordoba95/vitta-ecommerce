import type { Metadata } from "next";

// Applies to the whole /admin tree, login included: never indexed.
export const metadata: Metadata = {
  title: { template: "%s · Admin Vitta", default: "Admin Vitta" },
  robots: { index: false, follow: false },
};

export default function AdminRootLayout({ children }: LayoutProps<"/admin">) {
  return children;
}
