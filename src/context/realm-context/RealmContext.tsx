import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import _ from "lodash";
import React, { FunctionComponent, useEffect, useMemo, useState } from "react";
import { useRouteMatch } from "react-router-dom";
import { RecentUsed } from "../../components/realm-selector/recent-used";
import {
  DashboardParams,
  DashboardRoute,
} from "../../dashboard/routes/Dashboard";
import environment from "../../environment";
import useRequiredContext from "../../utils/useRequiredContext";
import { useAdminClient, useFetch } from "../auth/AdminClient";

type RealmContextType = {
  realm: string;
  realms: RealmRepresentation[];
  refresh: () => Promise<void>;
};

export const RealmContext = React.createContext<RealmContextType | undefined>(
  undefined
);

export const RealmContextProvider: FunctionComponent = ({ children }) => {
  const routeMatch = useRouteMatch<DashboardParams>(DashboardRoute.path);
  const realmParam = routeMatch?.params.realm;
  const realm = useMemo(
    () => realmParam ?? environment.loginRealm,
    [realmParam]
  );

  const [realms, setRealms] = useState<RealmRepresentation[]>([]);
  const adminClient = useAdminClient();
  const recentUsed = new RecentUsed();

  const updateRealmsList = (realms: RealmRepresentation[]) => {
    setRealms(_.sortBy(realms, "realm"));
    recentUsed.clean(realms.map((r) => r.realm!));
  };

  useFetch(
    () => adminClient.realms.find(),
    (realms) => updateRealmsList(realms),
    []
  );

  // Configure admin client to use selected realm when it changes.
  useEffect(() => adminClient.setConfig({ realmName: realm }), [realm]);

  // Keep track of recently used realms when selected realm changes.
  useEffect(() => recentUsed.setRecentUsed(realm), [realm]);

  return (
    <RealmContext.Provider
      value={{
        realm,
        realms,
        refresh: async () => {
          //this is needed otherwise the realm find function will not return
          //new or renamed realms because of the cached realms in the token (perhaps?)
          await adminClient.keycloak?.updateToken(Number.MAX_VALUE);
          const list = await adminClient.realms.find();
          updateRealmsList(list);
        },
      }}
    >
      {children}
    </RealmContext.Provider>
  );
};

export const useRealm = () => useRequiredContext(RealmContext);
