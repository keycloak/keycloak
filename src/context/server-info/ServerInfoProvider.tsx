import React, { createContext, FunctionComponent, useState } from "react";

import type { ServerInfoRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import { sortProviders } from "../../util";
import useRequiredContext from "../../utils/useRequiredContext";
import { useAdminClient, useFetch } from "../auth/AdminClient";

export const ServerInfoContext = createContext<
  ServerInfoRepresentation | undefined
>(undefined);

export const useServerInfo = () => useRequiredContext(ServerInfoContext);

export const useLoginProviders = () => {
  return sortProviders(useServerInfo().providers!["login-protocol"].providers);
};

export const ServerInfoProvider: FunctionComponent = ({ children }) => {
  const adminClient = useAdminClient();
  const [serverInfo, setServerInfo] = useState<ServerInfoRepresentation>({});

  useFetch(
    async () => {
      try {
        return await adminClient.serverInfo.find();
      } catch (error) {
        return {};
      }
    },
    setServerInfo,
    []
  );

  return (
    <ServerInfoContext.Provider value={serverInfo}>
      {children}
    </ServerInfoContext.Provider>
  );
};
