import React, { useState } from "react";

import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { KEY_PROVIDER_TYPE } from "../util";
import { RealmSettingsTabs } from "./RealmSettingsTabs";

const sortByPriority = (components: ComponentRepresentation[]) => {
  const sortedComponents = [...components].sort((a, b) => {
    const priorityA = Number(a.config?.priority);
    const priorityB = Number(b.config?.priority);

    return (
      (!isNaN(priorityB) ? priorityB : 0) - (!isNaN(priorityA) ? priorityA : 0)
    );
  });

  return sortedComponents;
};

export default function RealmSettingsSection() {
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [realmComponents, setRealmComponents] =
    useState<ComponentRepresentation[]>();
  const [key, setKey] = useState(0);

  const refresh = () => {
    setKey(key + 1);
  };

  useFetch(
    async () => {
      const realm = await adminClient.realms.findOne({ realm: realmName });
      const realmComponents = await adminClient.components.find({
        type: KEY_PROVIDER_TYPE,
        realm: realmName,
      });

      return { realm, realmComponents };
    },
    ({ realm, realmComponents }) => {
      setRealmComponents(sortByPriority(realmComponents));
      setRealm(realm);
    },
    [key]
  );

  if (!realm || !realmComponents) {
    return <KeycloakSpinner />;
  }
  return (
    <RealmSettingsTabs
      realm={realm}
      refresh={refresh}
      realmComponents={realmComponents}
    />
  );
}
