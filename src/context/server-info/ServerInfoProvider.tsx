import type { ServerInfoRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";
import React, { createContext, ReactNode } from "react";
import { DataLoader } from "../../components/data-loader/DataLoader";
import { sortProviders } from "../../util";
import useRequiredContext from "../../utils/useRequiredContext";
import { useAdminClient } from "../auth/AdminClient";

export const ServerInfoContext = createContext<
  ServerInfoRepresentation | undefined
>(undefined);

export const useServerInfo = () => useRequiredContext(ServerInfoContext);

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
        <ServerInfoContext.Provider value={serverInfo}>
          {children}
        </ServerInfoContext.Provider>
      )}
    </DataLoader>
  );
};
