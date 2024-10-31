import { PropsWithChildren, useEffect } from "react";

import {
  createNamedContext,
  useRequiredContext,
  useStoredState,
} from "@keycloak/keycloak-ui-shared";
import { useRealm } from "./realm-context/RealmContext";

const MAX_REALMS = 4;

export const RecentRealmsContext = createNamedContext<{
  recentRealms?: string[];
  removeRealm?: (realmName: string) => void;
}>("RecentRealmsContext", {});

export const RecentRealmsProvider = ({ children }: PropsWithChildren) => {
  const { realm } = useRealm();
  const [storedRealms, setStoredRealms] = useStoredState(
    localStorage,
    "recentRealms",
    [realm],
  );

  useEffect(() => {
    const newRealms = [...new Set([realm, ...storedRealms])];
    setStoredRealms(newRealms.slice(0, MAX_REALMS));
  }, [realm]);

  const removeRealm = (realmName: string) =>
    setStoredRealms(storedRealms.filter((r) => r !== realmName));

  return (
    <RecentRealmsContext.Provider
      value={{ recentRealms: storedRealms, removeRealm }}
    >
      {children}
    </RecentRealmsContext.Provider>
  );
};

export const useRecentRealms = () => useRequiredContext(RecentRealmsContext);
