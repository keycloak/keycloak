import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { sortBy } from "lodash";
import React, {
  createContext,
  FunctionComponent,
  useCallback,
  useMemo,
  useState,
} from "react";
import { RecentUsed } from "../components/realm-selector/recent-used";
import useRequiredContext from "../utils/useRequiredContext";
import { useAdminClient, useFetch } from "./auth/AdminClient";

type RealmsContextProps = {
  /** A list of all the realms. */
  realms: RealmRepresentation[];
  /** Refreshes the realms with the latest information. */
  refresh: () => Promise<void>;
};

export const RealmsContext = createContext<RealmsContextProps | undefined>(
  undefined
);

export const RealmsProvider: FunctionComponent = ({ children }) => {
  const adminClient = useAdminClient();
  const [realms, setRealms] = useState<RealmRepresentation[]>([]);
  const recentUsed = useMemo(() => new RecentUsed(), []);

  function updateRealms(realms: RealmRepresentation[]) {
    setRealms(sortBy(realms, "realm"));
    recentUsed.clean(realms.map(({ realm }) => realm!));
  }

  useFetch(
    () => adminClient.realms.find(),
    (realms) => updateRealms(realms),
    []
  );

  const refresh = useCallback(async () => {
    //this is needed otherwise the realm find function will not return
    //new or renamed realms because of the cached realms in the token (perhaps?)
    await adminClient.keycloak?.updateToken(Number.MAX_VALUE);
    updateRealms(await adminClient.realms.find());
  }, []);

  const value = useMemo<RealmsContextProps>(
    () => ({ realms, refresh }),
    [realms, refresh]
  );

  return (
    <RealmsContext.Provider value={value}>{children}</RealmsContext.Provider>
  );
};

export const useRealms = () => useRequiredContext(RealmsContext);
