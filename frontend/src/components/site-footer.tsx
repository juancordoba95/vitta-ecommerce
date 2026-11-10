import { InstagramIcon } from "@/components/icons";

const INSTAGRAM_URL = "https://www.instagram.com/vitta.bymarie/";

export function SiteFooter() {
  return (
    <footer className="mt-20 border-t border-charcoal/10">
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-4 px-4 py-12 text-center">
        <a
          href={INSTAGRAM_URL}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-2 text-xs tracking-label uppercase transition-opacity hover:opacity-60"
        >
          <InstagramIcon />
          @vitta.bymarie
        </a>
        <p className="font-script text-2xl text-charcoal/70">
          Vitta by Marie
        </p>
      </div>
    </footer>
  );
}
