/** Notice shown when the cart could not be revalidated, with a retry button. */
export function CartValidationError({
  onRetry,
  className = "",
}: {
  onRetry: () => void;
  className?: string;
}) {
  return (
    <div
      role="alert"
      className={`flex flex-col gap-2 rounded-soft bg-butter/70 px-4 py-3 text-sm ${className}`}
    >
      <p>
        No pudimos actualizar precios y stock. Te mostramos los últimos datos que tenemos.
      </p>
      <button
        type="button"
        onClick={onRetry}
        className="self-start border-b border-charcoal/40 pb-0.5 text-[0.65rem] tracking-label uppercase hover:border-charcoal"
      >
        Reintentar
      </button>
    </div>
  );
}
