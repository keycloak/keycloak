import { addTrailingSlash } from "../util";

// Mirrors the server's frontendUrl acceptance (HostnameV2Provider): only an
// absolute http(s) URL overrides the base, and it is normalized the same way
// the server normalizes it (lowercased scheme/host). Parsing rather than a
// prefix check keeps the displayed callback in step with what the server
// resolves — non-URL values the server discards fall back here too.
const parseHttpUrl = (value: string) => {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url : null;
  } catch {
    return null;
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
  const parsed = frontendUrl ? parseHttpUrl(frontendUrl) : null;
  if (parsed) {
    // Extend the path (as the server's UriBuilder.path does) so any query or
    // fragment on frontendUrl stays after the appended route.
    parsed.pathname = `${addTrailingSlash(parsed.pathname)}${brokerPath}`;
    return parsed.href;
  }
  return `${addTrailingSlash(serverBaseUrl)}${brokerPath}`;
};
