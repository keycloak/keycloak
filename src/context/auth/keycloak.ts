import Keycloak, { KeycloakInstance } from "keycloak-js";

const realm =
  new URLSearchParams(window.location.search).get("realm") || "master";

const keycloak: KeycloakInstance = Keycloak({
  url: "http://localhost:8180/auth/",
  realm: realm,
  clientId: "security-admin-console-v2",
});

export default async function (): Promise<KeycloakInstance> {
  await keycloak.init({ onLoad: "check-sso", pkceMethod: "S256" }).catch(() => {
    alert("failed to initialize keycloak");
  });
  return keycloak;
}
