export const DELIVERY_METHOD_PUSH_URI = "urn:ietf:rfc:8935";
export const DELIVERY_METHOD_POLL_URI = "urn:ietf:rfc:8936";

export const isPollDeliveryMethod = (method: string | undefined): boolean =>
  method === DELIVERY_METHOD_POLL_URI ||
  method === "https://schemas.openid.net/secevent/risc/delivery-method/poll";

/**
 * Best-effort URL validation for the receiver's push endpoint. Uses
 * the URL constructor (no regex juggling) and additionally requires
 * an http/https protocol — the SSF dispatcher only knows how to push
 * over HTTP, so accepting e.g. ftp:// or javascript: would just
 * dead-letter the queued events on first push attempt. An empty
 * string is treated as "not yet typed" and not validated here so the
 * field's existing isRequired check owns that case.
 */
export const isValidPushEndpointUrl = (value: string): boolean => {
  const trimmed = value.trim();
  if (trimmed === "") return true;
  try {
    const parsed = new URL(trimmed);
    return parsed.protocol === "http:" || parsed.protocol === "https:";
  } catch {
    return false;
  }
};
