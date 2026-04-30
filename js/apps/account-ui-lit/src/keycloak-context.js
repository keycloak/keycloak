import { createContext } from "@lit/context";
import Keycloak from "keycloak-js";
import { environment } from "./environment.js";

/** @type {import("@lit/context").Context<string, Keycloak | undefined>} */
export const keycloakContext = createContext("keycloak");

/**
 * @returns {Promise<Keycloak>}
 */
export async function initKeycloak() {
  const keycloak = new Keycloak({
    url: environment.serverBaseUrl,
    realm: environment.realm,
    clientId: environment.clientId,
  });

  await keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    checkLoginIframe: false,
  });

  if (!keycloak.authenticated) {
    await keycloak.login();
  }

  return keycloak;
}
