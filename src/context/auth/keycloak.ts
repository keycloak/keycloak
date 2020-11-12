import KcAdminClient from "keycloak-admin";

export default async function (): Promise<KcAdminClient> {
  const realm =
    new URLSearchParams(window.location.search).get("realm") || "master";

  const kcAdminClient = new KcAdminClient();

  try {
    await kcAdminClient.init(
      { onLoad: "check-sso", pkceMethod: "S256" },
      {
        url: "http://localhost:8180/auth/",
        realm: realm,
        clientId: "security-admin-console-v2",
      }
    );
    kcAdminClient.baseUrl = "";
  } catch (error) {
    alert("failed to initialize keycloak");
  }

  return kcAdminClient;
}
