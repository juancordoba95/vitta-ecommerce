import Link from "next/link";
import { Wordmark } from "@/components/wordmark";

export function SiteHeader() {
  return (
    <header className="border-b border-charcoal/10">
      <div className="mx-auto flex max-w-6xl justify-center px-4 py-6">
        <Link href="/" aria-label="Vitta by Marie, ir al inicio">
          <Wordmark />
        </Link>
      </div>
    </header>
  );
}
