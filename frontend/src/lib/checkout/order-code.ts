/**
 * No 0/O or 1/I, so the code is easy to read aloud and type back on WhatsApp. Exactly
 * 32 symbols: a random byte modulo 32 is unbiased.
 */
const ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
const CODE_LENGTH = 4;

export const ORDER_CODE_PATTERN = /^VT-[2-9A-HJ-NP-Z]{4}$/;

/**
 * Short, human-friendly order code such as `VT-4F7K`. It only identifies the
 * conversation on WhatsApp (nothing is stored on the server), so ~1M combinations are
 * plenty. Uses the Web Crypto API.
 */
export function generateOrderCode(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(CODE_LENGTH));
  let code = "";
  for (const byte of bytes) code += ALPHABET[byte % ALPHABET.length];
  return `VT-${code}`;
}
