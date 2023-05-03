import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useState } from "react";

import { adminClient } from "../admin-client";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { useFetch } from "../utils/useFetch";
import { useParams } from "../utils/useParams";
import { RealmSettingsTabs } from "./RealmSettingsTabs";
import type { RealmSettingsParams } from "./routes/RealmSettings";

export default function RealmSettingsSection() {
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
