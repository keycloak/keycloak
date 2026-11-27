/**
 * Parameters keycloak-js recognizes on a callback URL, and removes from it once the callback
 * has been handled.
 */
const AUTH_RESPONSE_PARAMS = [
  "code",
  "state",
  "session_state",
  "iss",
  "error",
  "error_description",
  "error_uri",
  "kc_action",
  "kc_action_status",
];

/**
 * Removes the parameters of an authorization response that cannot belong to this application.
 *
 * keycloak-js treats a URL as its own callback only when it carries `state` alongside `code` or
 * `error`, and strips the response parameters from the address bar when it does. A response
 * without `state` was never requested by this application — keycloak-js always sends one — so it
 * goes unrecognized and the parameters are left behind. They would then be sent as the
 * `redirect_uri` of the next authorization request made from the page, which the server rejects
 * for carrying OIDC response parameters, leaving the page unable to log in at all.
 *
 * @param url the current URL
 * @returns the URL with the response parameters removed, or `undefined` if it should be left as
 * it is, either because it carries no response or because the response belongs to this
 * application and keycloak-js will deal with it.
 */
export function stripForeignAuthResponse(url: string): string | undefined {
  const parsed = new URL(url);
  const params = parsed.searchParams;

  if (!params.has("code") && !params.has("error")) {
    return undefined;
  }

  if (params.has("state")) {
    return undefined;
  }

  for (const param of AUTH_RESPONSE_PARAMS) {
    params.delete(param);
  }

  return parsed.toString();
}
