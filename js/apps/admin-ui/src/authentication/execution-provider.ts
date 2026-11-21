import type { ExpandableExecution } from "./execution-model";

/** RFC3548 Base32 alphabet used by Keycloak's Base32 utility. */
const BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
const BASE32_LOOKUP = (() => {
  const table = new Array<number>(128).fill(0xff);
  for (let i = 0; i < BASE32_CHARS.length; i++) {
    table[BASE32_CHARS.charCodeAt(i)] = i;
    table[BASE32_CHARS.toLowerCase().charCodeAt(i)] = i;
  }
  return table;
})();

/**
 * Decode a Keycloak-style Base32 string (no padding), matching
 * `org.keycloak.models.utils.Base32.decode`.
 */
export function decodeBase32(base32: string): string {
  const bytes = new Uint8Array(Math.floor((base32.length * 5) / 8));
  let index = 0;
  let offset = 0;

  for (let i = 0; i < base32.length; i++) {
    const code = base32.charCodeAt(i);
    if (code >= BASE32_LOOKUP.length) {
      continue;
    }
    const digit = BASE32_LOOKUP[code];
    if (digit === 0xff) {
      continue;
    }

    if (index <= 3) {
      index = (index + 5) % 8;
      if (index === 0) {
        bytes[offset] |= digit;
        offset++;
        if (offset >= bytes.length) {
          break;
        }
      } else {
        bytes[offset] |= digit << (8 - index);
      }
    } else {
      index = (index + 5) % 8;
      bytes[offset] |= digit >>> index;
      offset++;
      if (offset >= bytes.length) {
        break;
      }
      bytes[offset] |= digit << (8 - index);
    }
  }

  return new TextDecoder().decode(bytes.subarray(0, offset));
}

/**
 * `getExecutions` Base32-encodes provider IDs that start with `script-`, but
 * `addExecutionToFlow` expects the raw provider ID. Recover the original value
 * when the encoded form is present.
 */
export function decodeExecutionProviderId(providerId: string): string {
  if (providerId.startsWith("script-")) {
    return providerId;
  }
  if (!/^[A-Za-z2-7]+$/.test(providerId)) {
    return providerId;
  }
  try {
    const decoded = decodeBase32(providerId);
    if (decoded.startsWith("script-")) {
      return decoded;
    }
  } catch {
    // Not a script provider encoding; keep the original id.
  }
  return providerId;
}

/**
 * Infer a subflow's flow provider from requirement choices returned by
 * `getExecutions` (see AuthenticationManagementResource.recurseExecutions).
 */
export function inferSubflowProviderId(
  execution: Pick<
    ExpandableExecution,
    "authenticationFlow" | "requirementChoices"
  >,
): string | undefined {
  if (!execution.authenticationFlow) {
    return undefined;
  }
  const choices = execution.requirementChoices || [];
  if (choices.includes("CONDITIONAL")) {
    return "basic-flow";
  }
  if (
    choices.includes("REQUIRED") &&
    choices.includes("DISABLED") &&
    !choices.includes("ALTERNATIVE")
  ) {
    return "form-flow";
  }
  if (choices.includes("ALTERNATIVE") && !choices.includes("CONDITIONAL")) {
    return "client-flow";
  }
  return "basic-flow";
}
