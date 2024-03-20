import Keycloak from "keycloak-js";

import environment from "./environment";

export const keycloak = new Keycloak({
  url: environment.authServerUrl,
  realm: environment.loginRealm,
  clientId: environment.clientId,
});

export async function initKeycloak() {
  const authenticated = await keycloak.init({
    onLoad: "check-sso",
  });

  // Force the user to login if not authenticated.
  if (!authenticated) {
    await keycloak.login();
  }
}
