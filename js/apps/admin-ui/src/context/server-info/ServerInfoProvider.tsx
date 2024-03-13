import type { ServerInfoRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import { PropsWithChildren, useState } from "react";
import { createNamedContext, useRequiredContext } from "ui-shared";

import { adminClient } from "../../admin-client";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { sortProviders } from "../../util";
import { useFetch } from "../../utils/useFetch";

export const ServerInfoContext = createNamedContext<
  ServerInfoRepresentation | undefined
>("ServerInfoContext", undefined);

export const useServerInfo = () => useRequiredContext(ServerInfoContext);

export const useLoginProviders = () =>
  sortProviders(useServerInfo().providers!["login-protocol"].providers);

export const ServerInfoProvider = ({ children }: PropsWithChildren) => {
  const [serverInfo, setServerInfo] = useState<ServerInfoRepresentation>();

  useFetch(adminClient.serverInfo.find, setServerInfo, []);

  if (!serverInfo) {
    return <KeycloakSpinner />;
  }

  return (
    <ServerInfoContext.Provider value={serverInfo}>
      {children}
    </ServerInfoContext.Provider>
  );
};
