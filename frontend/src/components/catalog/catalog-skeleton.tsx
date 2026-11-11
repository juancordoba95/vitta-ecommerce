/** Loading placeholder shown while the catalog streams in. */
export function CatalogSkeleton() {
  return (
    <div aria-hidden="true" className="animate-pulse">
      <div className="flex justify-center gap-2">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="h-9 w-24 rounded-full bg-charcoal/5" />
        ))}
      </div>
      <div className="mt-10 grid grid-cols-2 gap-x-4 gap-y-10 md:grid-cols-3 lg:grid-cols-4 lg:gap-x-6">
        {Array.from({ length: 8 }, (_, i) => (
          <div key={i}>
            <div className="aspect-4/5 rounded-soft bg-charcoal/5" />
            <div className="mt-3 h-3 w-3/4 rounded-soft bg-charcoal/5" />
            <div className="mt-2 h-3 w-1/3 rounded-soft bg-charcoal/5" />
          </div>
        ))}
      </div>
    </div>
  );
}
