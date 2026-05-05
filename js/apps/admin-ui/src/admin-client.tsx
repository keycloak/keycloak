import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import {
  createNamedContext,
  KeycloakSpinner,
  useEnvironment,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import type Keycloak from "keycloak-js";
import { PropsWithChildren, useEffect, useState } from "react";

import type { Environment } from "./environment";

export type AdminClientProps = {
  keycloak: Keycloak;
  adminClient: KeycloakAdminClient;
};

export const AdminClientContext = createNamedContext<
  AdminClientProps | undefined
>("AdminClientContext", undefined);

export const useAdminClient = () => useRequiredContext(AdminClientContext);

export async function initAdminClient(
  keycloak: Keycloak,
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

export const AdminClientProvider = ({ children }: PropsWithChildren) => {
  const { keycloak, environment } = useEnvironment<Environment>();
  const [adminClient, setAdminClient] = useState<KeycloakAdminClient>();

  useEffect(() => {
    const init = async () => {
      const client = await initAdminClient(keycloak, environment);
      setAdminClient(client);
    };
    init().catch(console.error);
  }, [environment, keycloak]);

  if (!adminClient) return <KeycloakSpinner />;
  return (
    <AdminClientContext.Provider value={{ keycloak, adminClient }}>
      {children}
    </AdminClientContext.Provider>
  );
};
