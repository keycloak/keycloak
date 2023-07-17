import Keycloak from "keycloak-js";
import { environment } from "./environment";

export const keycloak = new Keycloak({
  url: environment.authUrl,
  realm: environment.realm,
  clientId: environment.clientId,
});
