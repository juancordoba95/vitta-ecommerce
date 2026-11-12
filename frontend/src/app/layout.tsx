import type { Metadata } from "next";
import { Jost, Pinyon_Script } from "next/font/google";
import "./globals.css";

const jost = Jost({
  variable: "--font-jost",
  subsets: ["latin"],
});

const pinyonScript = Pinyon_Script({
  variable: "--font-pinyon-script",
  weight: "400",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Vitta by Marie · Lencería y accesorios",
  description:
    "Lencería y accesorios pensados para que te elijas todos los días.",
};

/** Document shell only: the store (`(store)`) and the admin (`admin`) bring their own frame. */
export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="es"
      className={`${jost.variable} ${pinyonScript.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col bg-cream text-charcoal">{children}</body>
    </html>
  );
}
