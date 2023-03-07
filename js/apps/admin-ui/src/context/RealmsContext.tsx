import { NetworkError } from "@keycloak/keycloak-admin-client";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { sortBy } from "lodash-es";
import { PropsWithChildren, useCallback, useMemo, useState } from "react";

import { createNamedContext, useRequiredContext } from "ui-shared";
import { useAdminClient, useFetch } from "./auth/AdminClient";

type RealmsContextProps = {
  /** A list of all the realms. */
  realms: RealmRepresentation[];
  /** Refreshes the realms with the latest information. */
  refresh: () => Promise<void>;
};

export const RealmsContext = createNamedContext<RealmsContextProps | undefined>(
  "RealmsContext",
  undefined
);

export const RealmsProvider = ({ children }: PropsWithChildren) => {
  const { keycloak, adminClient } = useAdminClient();
  const [realms, setRealms] = useState<RealmRepresentation[]>([]);
  const [refreshCount, setRefreshCount] = useState(0);

  function updateRealms(realms: RealmRepresentation[]) {
    setRealms(sortBy(realms, "realm"));
  }

  useFetch(
    async () => {
      // We don't want to fetch until the user has requested it, so let's ignore the initial mount.
      if (refreshCount === 0) {
        return [];
      }

      try {
        return await adminClient.realms.find({ briefRepresentation: true });
      } catch (error) {
        if (error instanceof NetworkError && error.response.status < 500) {
          return [];
        }

        throw error;
      }
    },
    (realms) => updateRealms(realms),
    [refreshCount]
  );

  const refresh = useCallback(async () => {
    //this is needed otherwise the realm find function will not return
    //new or renamed realms because of the cached realms in the token (perhaps?)
    await keycloak.updateToken(Number.MAX_VALUE);
    setRefreshCount((count) => count + 1);
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
