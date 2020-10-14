import Keycloak, { KeycloakInstance } from "keycloak-js";
const keycloak: KeycloakInstance = Keycloak("/keycloak.json");

export default async function (): Promise<KeycloakInstance> {
  await keycloak.init({ onLoad: "check-sso", pkceMethod: "S256" }).catch(() => {
    alert("failed to initialize keycloak");
  });
  return keycloak;
}
