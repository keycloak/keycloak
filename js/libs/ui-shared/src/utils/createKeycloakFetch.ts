import type Keycloak from "keycloak-js";

/**
 * Wraps {@link Keycloak.fetch} so concurrent token refreshes still get a DPoP upgrade.
 *
 * keycloak-js only replaces `Authorization: Bearer …` with a DPoP proof when the header
 * value exactly matches `Bearer ${keycloak.token}`. Admin/account UI requests capture the
 * token before calling fetch; if another request refreshes the token in between, that
 * equality check fails and the request is sent as Bearer with a DPoP-bound token, which
 * the server rejects. Re-syncing the header to the current token closes that race.
 */
export function createKeycloakFetch(keycloak: Keycloak): typeof fetch {
  return (input, init) => {
    const headers = new Headers(
      init?.headers ?? (input instanceof Request ? input.headers : undefined),
    );

    if (headers.has("Authorization") && keycloak.token) {
      headers.set("Authorization", `Bearer ${keycloak.token}`);
    }

    return keycloak.fetch(input, { ...init, headers });
  };
}
