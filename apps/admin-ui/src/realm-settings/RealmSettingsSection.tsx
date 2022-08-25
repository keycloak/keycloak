import { useState } from "react";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type { RealmSettingsParams } from "./routes/RealmSettings";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { RealmSettingsTabs } from "./RealmSettingsTabs";
import { useParams } from "react-router-dom";

export default function RealmSettingsSection() {
  const { adminClient } = useAdminClient();
  const { realm: realmName } = useParams<RealmSettingsParams>();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [key, setKey] = useState(0);

  const refresh = () => {
    setKey(key + 1);
    setRealm(undefined);
  };

  useFetch(() => adminClient.realms.findOne({ realm: realmName }), setRealm, [
    key,
  ]);

  if (!realm) {
    return <KeycloakSpinner />;
  }
  return <RealmSettingsTabs realm={realm} refresh={refresh} />;
}
