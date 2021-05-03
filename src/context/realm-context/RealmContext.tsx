import React, { useContext, useEffect, useState } from "react";
import _ from "lodash";

import RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { RecentUsed } from "../../components/realm-selector/recent-used";
import { useErrorHandler } from "react-error-boundary";
import { asyncStateFetch, useAdminClient } from "../auth/AdminClient";
import { WhoAmIContext } from "../whoami/WhoAmI";

type RealmContextType = {
  realm: string;
  setRealm: (realm: string) => void;
  realms: RealmRepresentation[];
  refresh: () => Promise<void>;
};

export const RealmContext = React.createContext<RealmContextType>({
  realm: "",
  setRealm: () => {},
  realms: [],
  refresh: () => Promise.resolve(),
});

type RealmContextProviderProps = { children: React.ReactNode };

export const RealmContextProvider = ({
  children,
}: RealmContextProviderProps) => {
  const { whoAmI } = useContext(WhoAmIContext);
  const [realm, setRealm] = useState(whoAmI.getHomeRealm());
  const [realms, setRealms] = useState<RealmRepresentation[]>([]);
  const adminClient = useAdminClient();
  const errorHandler = useErrorHandler();
  const recentUsed = new RecentUsed();

  const updateRealmsList = (realms: RealmRepresentation[]) => {
    setRealms(_.sortBy(realms, "realm"));
    recentUsed.clean(realms.map((r) => r.realm!));
  };

  useEffect(
    () =>
      asyncStateFetch(
        () => adminClient.realms.find(),
        (realms) => updateRealmsList(realms),
        errorHandler
      ),
    []
  );

  const set = (realm: string) => {
    if (
      realms.length === 0 ||
      realms.findIndex((r) => r.realm == realm) !== -1
    ) {
      recentUsed.setRecentUsed(realm);
      setRealm(realm);
      adminClient.setConfig({
        realmName: realm,
      });
    }
  };
  return (
    <RealmContext.Provider
      value={{
        realm,
        setRealm: set,
        realms,
        refresh: async () => {
          const list = await adminClient.realms.find();
          updateRealmsList(list);
        },
      }}
    >
      {children}
    </RealmContext.Provider>
  );
};

export const useRealm = () => useContext(RealmContext);
