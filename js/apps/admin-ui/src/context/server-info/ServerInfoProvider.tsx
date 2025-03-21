import { NetworkError } from "@keycloak/keycloak-admin-client";
import type { ServerInfoRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import {
  createNamedContext,
  useFetch,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useCallback, useState } from "react";
import { useAdminClient } from "../../admin-client";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { sortProviders } from "../../util";

export const ServerInfoContext = createNamedContext<
  ServerInfoRepresentation | undefined
>("ServerInfoContext", undefined);

export const useServerInfo = () => useRequiredContext(ServerInfoContext);

export const useLoginProviders = () =>
  sortProviders(useServerInfo().providers!["login-protocol"].providers);

export const ServerInfoProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();
  const [serverInfo, setServerInfo] = useState<ServerInfoRepresentation>();

  const findServerInfo = useCallback(async () => {
    try {
      const serverInfo = await adminClient.serverInfo.find();
      return serverInfo;
    } catch (error) {
      // The user is not allowed to view the server info
      if (error instanceof NetworkError && error.response?.status === 403) {
        return {};
      }

      throw error;
    }
  }, []);

  useFetch(findServerInfo, setServerInfo, []);

  if (!serverInfo) {
    return <KeycloakSpinner />;
  }

  return (
    <ServerInfoContext.Provider value={serverInfo}>
      {children}
    </ServerInfoContext.Provider>
  );
};
