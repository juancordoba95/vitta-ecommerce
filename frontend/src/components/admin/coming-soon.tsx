/** Placeholder for the admin sections built in M4.4. */
export function ComingSoon({ title }: { title: string }) {
  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-xs font-normal tracking-title uppercase">{title}</h1>
      <p className="rounded-soft border border-dashed border-charcoal/20 px-5 py-10 text-center text-sm text-charcoal/70">
        Muy pronto vas a poder gestionar esta sección desde acá.
      </p>
    </section>
  );
}
