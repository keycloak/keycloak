import type { ServerInfoRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import {
  createNamedContext,
  KeycloakSpinner,
  useFetch,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useState } from "react";
import { useAdminClient } from "../../admin-client";
import { sortProviders } from "../../util";

type ServerInfoContextType = {
  serverInfo: ServerInfoRepresentation;
  refresh: () => void;
};

export const ServerInfoContext = createNamedContext<
  ServerInfoContextType | undefined
>("ServerInfoContext", undefined);

export const useServerInfo = () => useRequiredContext(ServerInfoContext).serverInfo;

export const useRefreshServerInfo = () =>
  useRequiredContext(ServerInfoContext).refresh;

export const useLoginProviders = () =>
  sortProviders(useServerInfo().providers!["login-protocol"].providers);

export const ServerInfoProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();
  const [serverInfo, setServerInfo] = useState<ServerInfoRepresentation>();
  const [refreshToken, setRefreshToken] = useState(0);

  const refresh = () => setRefreshToken((t) => t + 1);

  useFetch(() => adminClient.serverInfo.find(), setServerInfo, [refreshToken]);

  if (!serverInfo) {
    return <KeycloakSpinner />;
  }

  return (
    <ServerInfoContext.Provider value={{ serverInfo, refresh }}>
      {children}
    </ServerInfoContext.Provider>
  );
};
