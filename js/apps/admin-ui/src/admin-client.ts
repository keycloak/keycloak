import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import {
  type KeycloakContext,
  type BaseEnvironment,
  createNamedContext,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import type { Environment } from "./environment";

type KeycloakInstance = KeycloakContext<BaseEnvironment>["keycloak"];

export type AdminClientProps = {
  keycloak: KeycloakInstance;
  adminClient: KeycloakAdminClient;
};

export const AdminClientContext = createNamedContext<
  AdminClientProps | undefined
>("AdminClientContext", undefined);

export const useAdminClient = () => useRequiredContext(AdminClientContext);

export async function initAdminClient(
  keycloak: KeycloakInstance,
  environment: Environment,
) {
  const adminClient = new KeycloakAdminClient();

  adminClient.setConfig({ realmName: environment.realm });
  adminClient.baseUrl = environment.adminBaseUrl;
  adminClient.registerTokenProvider({
    async getAccessToken() {
      try {
        await keycloak.updateToken(5);
      } catch {
        await keycloak.login();
      }

      return keycloak.token;
    },
  });

  return adminClient;
}
