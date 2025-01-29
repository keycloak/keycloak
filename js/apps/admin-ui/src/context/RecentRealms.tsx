import { PropsWithChildren, useEffect } from "react";

import {
  createNamedContext,
  useRequiredContext,
  useStoredState,
} from "@keycloak/keycloak-ui-shared";
import { useRealm } from "./realm-context/RealmContext";

const MAX_REALMS = 5;

export const RecentRealmsContext = createNamedContext<
  RealmNameRepresentation[] | undefined
>("RecentRealmsContext", undefined);

export type RealmNameRepresentation = {
  name: string;
  displayName?: string;
};

export const RecentRealmsProvider = ({ children }: PropsWithChildren) => {
  const { realmRepresentation: realm } = useRealm();

  const [storedRealms, setStoredRealms] = useStoredState(
    localStorage,
    "recentRealms",
    [{ name: "" }] as RealmNameRepresentation[],
  );

  useEffect(() => {
    if (storedRealms.map((r) => r.name).includes(realm?.realm || "")) {
      return;
    }
    const realms = [
      { name: realm?.realm || "", displayName: realm?.displayName },
      ...storedRealms.filter((r) => r.name !== ""),
    ];
    setStoredRealms(
      realms.length > MAX_REALMS ? realms.slice(0, MAX_REALMS) : realms,
    );
  }, [realm]);

  return (
    <RecentRealmsContext.Provider value={storedRealms}>
      {children}
    </RecentRealmsContext.Provider>
  );
};

export const useRecentRealms = () => useRequiredContext(RecentRealmsContext);
