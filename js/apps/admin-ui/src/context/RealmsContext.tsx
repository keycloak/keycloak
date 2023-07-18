import { NetworkError } from "@keycloak/keycloak-admin-client";
import { PropsWithChildren, useCallback, useMemo, useState } from "react";
import { createNamedContext, useRequiredContext } from "ui-shared";

import { keycloak } from "../keycloak";
import { useFetch } from "../utils/useFetch";
import { fetchAdminUI } from "./auth/admin-ui-endpoint";

type RealmsContextProps = {
  /** A list of all the realms. */
  realms: string[];
  /** Refreshes the realms with the latest information. */
  refresh: () => Promise<void>;
};

export const RealmsContext = createNamedContext<RealmsContextProps | undefined>(
  "RealmsContext",
  undefined,
);

export const RealmsProvider = ({ children }: PropsWithChildren) => {
  const [realms, setRealms] = useState<string[]>([]);
  const [refreshCount, setRefreshCount] = useState(0);

  function updateRealms(realms: string[]) {
    setRealms(realms.sort());
  }

  useFetch(
    async () => {
      // We don't want to fetch until the user has requested it, so let's ignore the initial mount.
      if (refreshCount === 0) {
        return [];
      }

      try {
        return await fetchAdminUI<string[]>("ui-ext/realms", {});
      } catch (error) {
        if (error instanceof NetworkError && error.response.status < 500) {
          return [];
        }

        throw error;
      }
    },
    (realms) => updateRealms(realms),
    [refreshCount],
  );

  const refresh = useCallback(async () => {
    //this is needed otherwise the realm find function will not return
    //new or renamed realms because of the cached realms in the token (perhaps?)
    await keycloak.updateToken(Number.MAX_VALUE);
    setRefreshCount((count) => count + 1);
  }, []);

  const value = useMemo<RealmsContextProps>(
    () => ({ realms, refresh }),
    [realms, refresh],
  );

  return (
    <RealmsContext.Provider value={value}>{children}</RealmsContext.Provider>
  );
};

export const useRealms = () => useRequiredContext(RealmsContext);
