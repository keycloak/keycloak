import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  createNamedContext,
  useFetch,
  useRequiredContext,
  useStoredState,
} from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useEffect } from "react";
import { useAdminClient } from "../admin-client";
import { useRealm } from "./realm-context/RealmContext";
import { fetchAdminUI } from "./auth/admin-ui-endpoint";

const MAX_REALMS = 3;

export const RecentRealmsContext = createNamedContext<
  RealmNameRepresentation[] | undefined
>("RecentRealmsContext", undefined);

export type RealmNameRepresentation = {
  name: string;
  displayName?: string;
};

function convertRealmToNameRepresentation(
  realm?: RealmRepresentation,
): RealmNameRepresentation {
  return { name: realm?.realm || "", displayName: realm?.displayName || "" };
}

export const RecentRealmsProvider = ({ children }: PropsWithChildren) => {
  const { realmRepresentation: realm } = useRealm();
  const { adminClient } = useAdminClient();

  const [storedRealms, setStoredRealms] = useStoredState(
    localStorage,
    "recentRealms",
    [{ name: "" }] as RealmNameRepresentation[],
  );

  useFetch(
    () =>
      Promise.all(
        storedRealms.map(async (r) => {
          if (!r.name) {
            return undefined;
          }
          try {
            return (
              await fetchAdminUI<RealmNameRepresentation[]>(
                adminClient,
                "ui-ext/realms/names",
                { search: r.name },
              )
            )[0];
          } catch (error) {
            console.info("recent realm not found", error);
            return undefined;
          }
        }),
      ),
    (realms) => setStoredRealms(realms.filter((r) => !!r)),
    [realm?.realm === "master"],
  );

  useEffect(() => {
    if (
      storedRealms.map((r) => r.name).includes(realm?.realm || "") ||
      !realm
    ) {
      return;
    }
    setStoredRealms(
      [
        convertRealmToNameRepresentation(realm),
        ...storedRealms.filter((r) => r.name !== ""),
      ].slice(0, MAX_REALMS),
    );
  }, [realm]);

  return (
    <RecentRealmsContext.Provider value={storedRealms}>
      {children}
    </RecentRealmsContext.Provider>
  );
};

export const useRecentRealms = () => useRequiredContext(RecentRealmsContext);
