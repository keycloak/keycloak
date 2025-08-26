import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  createNamedContext,
  KeycloakSpinner,
  useEnvironment,
  useFetch,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useEffect, useState } from "react";
import { useAdminClient } from "../../admin-client";
import { i18n } from "../../i18n/i18n";
import { useHash } from "./useHash";

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

  const locationRealm = useHash();
  const realm = locationRealm?.split("/")[1] ?? environment.realm;

  // Configure admin client to use selected realm when it changes.
  useEffect(() => {
    void (async () => {
      adminClient.setConfig({ realmName: realm });
      const namespace = encodeURIComponent(realm);
      await i18n.loadNamespaces(namespace);
      i18n.setDefaultNamespace(namespace);
    })();
  }, [realm]);
  useFetch(
    () => adminClient.realms.findOne({ realm }),
    setRealmRepresentation,
    [realm, key],
  );

  if (!realmRepresentation) {
    return <KeycloakSpinner />;
  }

  return (
    <RealmContext.Provider value={{ realm, realmRepresentation, refresh }}>
      {children}
    </RealmContext.Provider>
  );
};

export const useRealm = () => useRequiredContext(RealmContext);
