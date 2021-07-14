import KcAdminClient from "keycloak-admin";
import { homeRealm, isDevMode, authUri } from "../../util";

export default async function (): Promise<KcAdminClient> {
  const kcAdminClient = new KcAdminClient();
  try {
    await kcAdminClient.init(
      { onLoad: "check-sso", pkceMethod: "S256" },
      {
        url: authUri(),
        realm: homeRealm(),
        clientId: isDevMode
          ? "security-admin-console-v2"
          : "security-admin-console",
      }
    );
    kcAdminClient.setConfig({ realmName: homeRealm() });

    kcAdminClient.baseUrl = authUri();
  } catch (error) {
    alert("failed to initialize keycloak");
  }

  return kcAdminClient;
}
