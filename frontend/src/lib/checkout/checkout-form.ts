export type DeliveryMethod = "pickup" | "shipping";

export type CheckoutFormValues = {
  name: string;
  /** Empty until the customer picks one. */
  delivery: DeliveryMethod | "";
  address: string;
  locality: string;
  notes: string;
};

export type CheckoutField = keyof CheckoutFormValues;

export type CheckoutFormErrors = Partial<Record<CheckoutField, string>>;

export const EMPTY_CHECKOUT_FORM: CheckoutFormValues = {
  name: "",
  delivery: "",
  address: "",
  locality: "",
  notes: "",
};

/** Max lengths, also set as `maxLength` on the inputs. */
export const CHECKOUT_FIELD_LIMITS = {
  name: 80,
  address: 120,
  locality: 80,
  notes: 500,
} as const satisfies Partial<Record<CheckoutField, number>>;

/** Field order on screen: the first invalid one gets the focus. */
export const CHECKOUT_FIELD_ORDER: CheckoutField[] = [
  "name",
  "delivery",
  "address",
  "locality",
  "notes",
];

function tooLong(field: keyof typeof CHECKOUT_FIELD_LIMITS, value: string) {
  return value.trim().length > CHECKOUT_FIELD_LIMITS[field]
    ? `Usá hasta ${CHECKOUT_FIELD_LIMITS[field]} caracteres.`
    : undefined;
}

/**
 * Client-side validation with one message per field. Name and delivery method are
 * required; address and locality only for shipping; notes are optional.
 */
export function validateCheckoutForm(values: CheckoutFormValues): CheckoutFormErrors {
  const errors: CheckoutFormErrors = {};

  if (!values.name.trim()) errors.name = "Ingresá tu nombre.";
  else errors.name = tooLong("name", values.name);

  if (values.delivery === "") {
    errors.delivery = "Elegí cómo querés recibir tu pedido.";
  }

  if (values.delivery === "shipping") {
    if (!values.address.trim()) errors.address = "Ingresá la dirección de entrega.";
    else errors.address = tooLong("address", values.address);

    if (!values.locality.trim()) errors.locality = "Ingresá tu localidad.";
    else errors.locality = tooLong("locality", values.locality);
  }

  errors.notes = tooLong("notes", values.notes);

  // Drop fields without an error so `Object.keys(errors).length` means "invalid".
  for (const field of CHECKOUT_FIELD_ORDER) {
    if (errors[field] === undefined) delete errors[field];
  }
  return errors;
}

export function hasErrors(errors: CheckoutFormErrors): boolean {
  return Object.keys(errors).length > 0;
}
