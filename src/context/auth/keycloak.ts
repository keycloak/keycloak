import KcAdminClient from "@keycloak/keycloak-admin-client";
import environment from "../../environment";

export default async function (): Promise<KcAdminClient> {
  const kcAdminClient = new KcAdminClient();
  try {
    await kcAdminClient.init(
      { onLoad: "check-sso", pkceMethod: "S256" },
      {
        url: environment.authUrl,
        realm: environment.loginRealm,
        clientId: environment.isRunningAsTheme
          ? "security-admin-console"
          : "security-admin-console-v2",
      }
    );
    kcAdminClient.setConfig({ realmName: environment.loginRealm });

    kcAdminClient.baseUrl = environment.authUrl;
  } catch (error) {
    alert("failed to initialize keycloak");
  }

  return kcAdminClient;
}
