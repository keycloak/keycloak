import { addTrailingSlash } from "../util";

// Validate the scheme with URL (http/https only), but build from the original
// string. The WHATWG URL parser canonicalizes the path (e.g. collapses `/a/..`),
// which would diverge from the server's UriBuilder; since redirect URI matching
// is exact, the displayed value must preserve frontendUrl byte-for-byte.
const isHttpUrl = (value: string) => {
  try {
    const { protocol } = new URL(value);
    return protocol === "http:" || protocol === "https:";
  } catch {
    return false;
  }
};

/**
 * Builds the broker callback URL shown for an identity provider.
 *
 * A realm may override its public-facing base URL via the `frontendUrl`
 * attribute (Realm Settings → General). That override drives the redirect_uri
 * Keycloak actually sends to the external IdP, so the admin console must show
 * it too. `serverBaseUrl` reflects the console-hosting realm, and is used only
 * when the realm has no absolute `frontendUrl` of its own.
 */
export const identityProviderRedirectUrl = (
  alias: string,
  realm: string,
  serverBaseUrl: string,
  frontendUrl?: string,
) => {
  const brokerPath = `realms/${realm}/broker/${alias}/endpoint`;
  const base =
    frontendUrl && isHttpUrl(frontendUrl) ? frontendUrl : serverBaseUrl;
  // Insert the route into the path, before any query or fragment, the way the
  // server's UriBuilder.path does.
  const queryOrFragment = base.search(/[?#]/);
  const path = queryOrFragment === -1 ? base : base.slice(0, queryOrFragment);
  const suffix = queryOrFragment === -1 ? "" : base.slice(queryOrFragment);
  return `${addTrailingSlash(path)}${brokerPath}${suffix}`;
};
