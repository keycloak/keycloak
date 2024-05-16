import { NetworkError } from "@keycloak/keycloak-admin-client";
import {
  createNamedContext,
  label,
  useEnvironment,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useFetch } from "../utils/useFetch";
import useLocaleSort from "../utils/useLocaleSort";
import { fetchAdminUI } from "./auth/admin-ui-endpoint";

type RealmsContextProps = {
  /** A list of all the realms. */
  realms: RealmNameRepresentation[];
  /** Refreshes the realms with the latest information. */
  refresh: () => Promise<void>;
};

export interface RealmNameRepresentation {
  name: string;
  displayName?: string;
}

export const RealmsContext = createNamedContext<RealmsContextProps | undefined>(
  "RealmsContext",
  undefined,
);

export const RealmsProvider = ({ children }: PropsWithChildren) => {
  const { keycloak } = useEnvironment();
  const { adminClient } = useAdminClient();

  const [realms, setRealms] = useState<RealmNameRepresentation[]>([]);
  const [refreshCount, setRefreshCount] = useState(0);
  const localeSort = useLocaleSort();
  const { t } = useTranslation();

  function updateRealms(realms: RealmNameRepresentation[]) {
    setRealms(localeSort(realms, (r) => label(t, r.displayName, r.name)));
  }

  useFetch(
    async () => {
      try {
        return await fetchAdminUI<RealmNameRepresentation[]>(
          adminClient,
          "ui-ext/realms/names",
          {},
        );
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
