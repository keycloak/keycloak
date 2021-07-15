import React, { useContext, useState } from "react";
import _ from "lodash";

import type RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { RecentUsed } from "../../components/realm-selector/recent-used";
import { useAdminClient, useFetch } from "../auth/AdminClient";
import environment from "../../environment";

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
  const [realm, setRealm] = useState(environment.loginRealm);
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
