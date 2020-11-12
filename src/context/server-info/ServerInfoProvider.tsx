import React, { createContext, ReactNode, useContext } from "react";
import { ServerInfoRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";

import { sortProviders } from "../../util";
import { DataLoader } from "../../components/data-loader/DataLoader";
import { useAdminClient } from "../auth/AdminClient";

export const ServerInfoContext = createContext<ServerInfoRepresentation>(
  {} as ServerInfoRepresentation
);

export const useServerInfo = () => useContext(ServerInfoContext);

export const useLoginProviders = () => {
  return sortProviders(useServerInfo().providers!["login-protocol"].providers);
};

export const ServerInfoProvider = ({ children }: { children: ReactNode }) => {
  const adminClient = useAdminClient();
  const loader = async () => {
    return await adminClient.serverInfo.find();
  };
  return (
    <DataLoader loader={loader}>
      {(serverInfo) => (
        <ServerInfoContext.Provider value={serverInfo.data}>
          {children}
        </ServerInfoContext.Provider>
      )}
    </DataLoader>
  );
};
