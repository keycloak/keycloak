import { PropsWithChildren, useEffect, useMemo } from "react";

import {
  createNamedContext,
  useRequiredContext,
  useStoredState,
} from "@keycloak/keycloak-ui-shared";
import { useRealm } from "./realm-context/RealmContext";
import { RealmNameRepresentation, useRealms } from "./RealmsContext";

const MAX_REALMS = 4;

export const RecentRealmsContext = createNamedContext<string[] | undefined>(
  "RecentRealmsContext",
  undefined,
);

export const RecentRealmsProvider = ({ children }: PropsWithChildren) => {
  const { realms } = useRealms();
  const { realm } = useRealm();
  const [storedRealms, setStoredRealms] = useStoredState(
    localStorage,
    "recentRealms",
    [realm],
  );

  const recentRealms = useMemo(
    () => filterRealmNames(realms, storedRealms),
    [realms, storedRealms],
  );

  useEffect(() => {
    const newRealms = [...new Set([realm, ...recentRealms])];
    setStoredRealms(newRealms.slice(0, MAX_REALMS));
  }, [realm]);

  return (
    <RecentRealmsContext.Provider value={recentRealms}>
      {children}
    </RecentRealmsContext.Provider>
  );
};

export const useRecentRealms = () => useRequiredContext(RecentRealmsContext);

function filterRealmNames(
  realms: RealmNameRepresentation[],
  storedRealms: string[],
) {
  // If no realms have been set yet we can't filter out any non-existent realm names.
  if (realms.length === 0) {
    return storedRealms;
  }

  // Only keep realm names that actually still exist.
  return storedRealms.filter((realm) => {
    return realms.some((r) => r.name === realm);
  });
}
