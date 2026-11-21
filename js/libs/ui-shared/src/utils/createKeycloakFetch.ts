import type Keycloak from "keycloak-js";

/**
 * Wraps {@link Keycloak.fetch} so concurrent token refreshes still get a DPoP upgrade.
 *
 * keycloak-js only replaces `Authorization: Bearer …` with a DPoP proof when the header
 * value exactly matches `Bearer ${keycloak.token}`. Admin/account UI requests capture the
 * token before calling fetch; if another request refreshes the token in between, that
 * equality check fails and the request is sent as Bearer with a DPoP-bound token, which
 * the server rejects. Re-syncing a stale `Bearer` header to the current token closes that
 * race. Non-Bearer schemes (for example `DPoP`) are left unchanged.
 */
export function createKeycloakFetch(keycloak: Keycloak): typeof fetch {
  return (input, init) => {
    const headers = new Headers(
      init?.headers ?? (input instanceof Request ? input.headers : undefined),
    );

    // Only resync console Bearer tokens. Leave DPoP or other schemes alone so
    // callers that already upgraded the header are not rewritten back to Bearer.
    const authorization = headers.get("Authorization");
    if (
      keycloak.token &&
      authorization?.startsWith("Bearer ") &&
      authorization !== `Bearer ${keycloak.token}`
    ) {
      headers.set("Authorization", `Bearer ${keycloak.token}`);
    }

    return keycloak.fetch(input, { ...init, headers });
  };
}
