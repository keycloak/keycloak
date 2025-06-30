import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  createNamedContext,
  useEnvironment,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useEffect, useMemo, useState } from "react";
import { useMatch } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { DashboardRouteWithRealm } from "../../dashboard/routes/Dashboard";
import { useFetch } from "../../utils/useFetch";
import { i18n } from "../../i18n/i18n";

type RealmContextType = {
  realm: string;
  realmRepresentation?: RealmRepresentation;
  refresh: () => void;
};

export const RealmContext = createNamedContext<RealmContextType | undefined>(
  "RealmContext",
  undefined,
);

export const RealmContextProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();
  const { environment } = useEnvironment();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [realmRepresentation, setRealmRepresentation] =
    useState<RealmRepresentation>();

  const routeMatch = useMatch({
    path: DashboardRouteWithRealm.path,
    end: false,
  });

  const realmParam = routeMatch?.params.realm;
  const realm = useMemo(
    () => decodeURIComponent(realmParam ?? environment.realm),
    [realmParam],
  );

  // Configure admin client to use selected realm when it changes.
  useEffect(() => {
    (async () => {
      adminClient.setConfig({ realmName: realm });
      await i18n.loadNamespaces(realm);
      i18n.setDefaultNamespace(realm);
    })();
  }, [realm]);
  useFetch(
    () => adminClient.realms.findOne({ realm }),
    setRealmRepresentation,
    [realm, key],
  );

  return (
    <RealmContext.Provider value={{ realm, realmRepresentation, refresh }}>
      {children}
    </RealmContext.Provider>
  );
};

export const useRealm = () => useRequiredContext(RealmContext);
