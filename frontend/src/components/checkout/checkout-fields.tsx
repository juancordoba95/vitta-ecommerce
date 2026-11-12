import type { ReactNode } from "react";
import {
  CHECKOUT_FIELD_LIMITS,
  type CheckoutField,
  type CheckoutFormErrors,
  type CheckoutFormValues,
  type DeliveryMethod,
} from "@/lib/checkout/checkout-form";

type CheckoutFieldsProps = {
  values: CheckoutFormValues;
  /** Only the errors that should be visible right now. */
  errors: CheckoutFormErrors;
  disabled: boolean;
  onChange: (field: CheckoutField, value: string) => void;
};

const labelClass = "text-[0.65rem] font-normal tracking-label uppercase";
const inputClass =
  "w-full rounded-soft border border-charcoal/20 bg-white/70 px-4 text-base text-charcoal transition-colors placeholder:text-charcoal/40 hover:border-charcoal/40 focus:border-charcoal aria-invalid:border-danger disabled:opacity-60 sm:text-sm";

const DELIVERY_OPTIONS: { value: DeliveryMethod; label: string; hint: string }[] = [
  { value: "pickup", label: "Retiro", hint: "Coordinamos el punto de retiro por WhatsApp." },
  { value: "shipping", label: "Envío", hint: "Te lo llevamos a tu casa. El costo lo coordinamos por WhatsApp." },
];

/** `id` of the element that gets the focus when a field is invalid. */
export function fieldFocusId(field: CheckoutField): string {
  return field === "delivery" ? "checkout-delivery-pickup" : `checkout-${field}`;
}

function FieldError({ id, message }: { id: string; message?: string }) {
  return message ? (
    <p id={id} className="text-xs text-danger">
      {message}
    </p>
  ) : null;
}

function TextField({
  field,
  label,
  optional = false,
  autoComplete,
  multiline = false,
  values,
  errors,
  disabled,
  onChange,
}: CheckoutFieldsProps & {
  field: "name" | "address" | "locality" | "notes";
  label: ReactNode;
  optional?: boolean;
  autoComplete?: string;
  multiline?: boolean;
}) {
  const id = `checkout-${field}`;
  const errorId = `${id}-error`;
  const error = errors[field];
  const shared = {
    id,
    name: field,
    value: values[field],
    maxLength: CHECKOUT_FIELD_LIMITS[field],
    disabled,
    required: !optional,
    "aria-invalid": error ? true : undefined,
    "aria-describedby": error ? errorId : undefined,
  } as const;

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className={labelClass}>
        {label}
        {optional && <span className="ml-2 text-charcoal/50 normal-case tracking-normal">(opcional)</span>}
      </label>
      {multiline ? (
        <textarea
          {...shared}
          rows={3}
          onChange={(event) => onChange(field, event.target.value)}
          className={`${inputClass} resize-y py-3`}
          placeholder="Por ejemplo: horarios para coordinar la entrega"
        />
      ) : (
        <input
          {...shared}
          type="text"
          autoComplete={autoComplete}
          onChange={(event) => onChange(field, event.target.value)}
          className={`${inputClass} h-12`}
        />
      )}
      <FieldError id={errorId} message={error} />
    </div>
  );
}

/** Customer details: name, delivery method, address/locality (shipping) and notes. */
export function CheckoutFields(props: CheckoutFieldsProps) {
  const { values, errors, disabled, onChange } = props;
  const isShipping = values.delivery === "shipping";

  return (
    <div className="flex flex-col gap-6">
      <TextField {...props} field="name" label="Nombre" autoComplete="name" />

      <fieldset
        aria-describedby={errors.delivery ? "checkout-delivery-error" : undefined}
        className="flex flex-col gap-2"
      >
        <legend className={`${labelClass} mb-2`}>Entrega</legend>
        <div className="grid gap-3 sm:grid-cols-2">
          {DELIVERY_OPTIONS.map((option) => (
            <label
              key={option.value}
              className={`flex cursor-pointer gap-3 rounded-soft border bg-white/70 px-4 py-3.5 transition-colors hover:border-charcoal/50 has-checked:border-charcoal has-checked:bg-white has-focus-visible:outline has-focus-visible:outline-1 has-focus-visible:outline-offset-3 has-focus-visible:outline-charcoal ${
                errors.delivery ? "border-danger" : "border-charcoal/20"
              }`}
            >
              <input
                type="radio"
                id={`checkout-delivery-${option.value}`}
                name="delivery"
                value={option.value}
                checked={values.delivery === option.value}
                onChange={() => onChange("delivery", option.value)}
                disabled={disabled}
                required
                className="mt-0.5 size-4 shrink-0 accent-charcoal focus-visible:outline-none"
              />
              <span className="flex flex-col gap-1">
                <span className="text-sm font-normal">{option.label}</span>
                <span className="text-xs text-charcoal/65">{option.hint}</span>
              </span>
            </label>
          ))}
        </div>
        <FieldError id="checkout-delivery-error" message={errors.delivery} />
      </fieldset>

      {isShipping && (
        <div className="grid gap-6 sm:grid-cols-2">
          <TextField
            {...props}
            field="address"
            label="Dirección"
            autoComplete="street-address"
          />
          <TextField
            {...props}
            field="locality"
            label="Localidad"
            autoComplete="address-level2"
          />
        </div>
      )}

      <TextField {...props} field="notes" label="Notas" optional multiline />
    </div>
  );
}
