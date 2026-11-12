/** Loading placeholder shown while the product detail streams in. */
export function ProductDetailSkeleton() {
  return (
    <div
      aria-hidden="true"
      className="grid animate-pulse gap-8 md:grid-cols-2 md:gap-12"
    >
      <div className="flex flex-col gap-3">
        <div className="aspect-4/5 rounded-soft bg-charcoal/5" />
        <div className="grid grid-cols-4 gap-2 sm:gap-3">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="aspect-4/5 rounded-soft bg-charcoal/5" />
          ))}
        </div>
      </div>
      <div className="flex flex-col gap-4 md:pt-4">
        <div className="h-3 w-20 rounded-soft bg-charcoal/5" />
        <div className="h-5 w-3/4 rounded-soft bg-charcoal/5" />
        <div className="h-5 w-1/3 rounded-soft bg-charcoal/5" />
        <div className="mt-4 flex gap-2">
          {Array.from({ length: 3 }, (_, i) => (
            <div key={i} className="size-11 rounded-full bg-charcoal/5" />
          ))}
        </div>
        <div className="mt-4 h-12 w-full rounded-full bg-charcoal/5 sm:max-w-xs" />
      </div>
    </div>
  );
}
