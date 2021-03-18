import KcAdminClient from "keycloak-admin";

export default async function (): Promise<KcAdminClient> {
  const realm =
    new URLSearchParams(window.location.search).get("realm") || "master";

  const kcAdminClient = new KcAdminClient();

  const authContext = "/auth";
  const keycloakAuthUrl = window.location.origin + authContext;
  const devMode = !window.location.pathname.startsWith("/adminv2");
  try {
    await kcAdminClient.init(
      { onLoad: "check-sso", pkceMethod: "S256" },
      {
        url: devMode ? "http://localhost:8180/auth" : keycloakAuthUrl,
        realm: realm,
        clientId: "security-admin-console-v2",
      }
    );
    kcAdminClient.setConfig({ realmName: realm });
    kcAdminClient.baseUrl = authContext;
  } catch (error) {
    alert("failed to initialize keycloak");
  }

  return kcAdminClient;
}
