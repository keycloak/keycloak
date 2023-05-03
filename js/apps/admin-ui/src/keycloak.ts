import Keycloak from "keycloak-js";

import environment from "./environment";

export const keycloak = new Keycloak({
  url: environment.authServerUrl,
  realm: environment.loginRealm,
  clientId: environment.isRunningAsTheme
    ? "security-admin-console"
    : "security-admin-console-v2",
});
