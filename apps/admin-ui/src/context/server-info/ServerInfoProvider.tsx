import type { ServerInfoRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import { PropsWithChildren, useState } from "react";

import { sortProviders } from "../../util";
import { createNamedContext } from "../../utils/createNamedContext";
import useRequiredContext from "../../utils/useRequiredContext";
import { useAdminClient, useFetch } from "../auth/AdminClient";

export const ServerInfoContext = createNamedContext<
  ServerInfoRepresentation | undefined
>("ServerInfoContext", undefined);

export const useServerInfo = () => useRequiredContext(ServerInfoContext);

export const useLoginProviders = () =>
  sortProviders(useServerInfo().providers!["login-protocol"].providers);

export const ServerInfoProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();
  const [serverInfo, setServerInfo] = useState<ServerInfoRepresentation>({});

  useFetch(adminClient.serverInfo.find, setServerInfo, []);

  return (
    <ServerInfoContext.Provider value={serverInfo}>
      {children}
    </ServerInfoContext.Provider>
  );
};
