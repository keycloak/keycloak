import React, { useState } from "react";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { RealmSettingsTabs } from "./RealmSettingsTabs";

export default function RealmSettingsSection() {
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [key, setKey] = useState(0);

  const refresh = () => {
    setKey(key + 1);
  };

  useFetch(() => adminClient.realms.findOne({ realm: realmName }), setRealm, [
    key,
  ]);

  if (!realm) {
    return <KeycloakSpinner />;
  }
  return <RealmSettingsTabs realm={realm} refresh={refresh} />;
}
