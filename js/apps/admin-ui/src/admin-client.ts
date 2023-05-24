import KeycloakAdminClient from "@keycloak/keycloak-admin-client";

import environment from "./environment";
import { keycloak } from "./keycloak";

export const adminClient = new KeycloakAdminClient();

adminClient.setConfig({ realmName: environment.loginRealm });
adminClient.baseUrl = environment.authUrl;
adminClient.registerTokenProvider({
  async getAccessToken() {
    try {
      await keycloak.updateToken(5);
    } catch (error) {
      keycloak.login();
    }

    return keycloak.token;
  },
});
