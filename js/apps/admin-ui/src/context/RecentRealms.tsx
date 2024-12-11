import { PropsWithChildren } from "react";

import {
  createNamedContext,
  useFetch,
  useRequiredContext,
  useStoredState,
} from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useRealm } from "./realm-context/RealmContext";

const MAX_REALMS = 4;

export const RecentRealmsContext = createNamedContext<string[] | undefined>(
  "RecentRealmsContext",
  undefined,
);

export const RecentRealmsProvider = ({ children }: PropsWithChildren) => {
  const { realm } = useRealm();
  const { adminClient } = useAdminClient();

  const [storedRealms, setStoredRealms] = useStoredState(
    localStorage,
    "recentRealms",
    [realm],
  );

  useFetch(
    () => {
      return Promise.all(
        [...new Set([realm, ...storedRealms])].map(async (realm) => {
          try {
            const response = await adminClient.realms.findOne({ realm });
            if (response) {
              return response.realm;
            }
          } catch {
            return undefined;
          }
        }),
      );
    },
    (realms) => {
      const newRealms = realms.filter((r) => r) as string[];
      setStoredRealms(newRealms.slice(0, MAX_REALMS));
    },
    [realm],
  );

  return (
    <RecentRealmsContext.Provider value={storedRealms}>
      {children}
    </RecentRealmsContext.Provider>
  );
};

export const useRecentRealms = () => useRequiredContext(RecentRealmsContext);
