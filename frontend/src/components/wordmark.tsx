/**
 * Text version of the Vitta by Marie logo. Swap for the image logo once a cropped
 * PNG/SVG with a transparent background is available.
 */
export function Wordmark({ className = "" }: { className?: string }) {
  return (
    <span
      className={`inline-flex flex-col items-center leading-none ${className}`}
    >
      <span className="font-script text-5xl">Vitta</span>
      <span className="mt-1 pl-[0.45em] text-[0.65rem] font-normal tracking-[0.45em] uppercase">
        by Marie
      </span>
    </span>
  );
}
