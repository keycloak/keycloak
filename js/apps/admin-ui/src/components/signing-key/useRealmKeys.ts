import { useState } from "react";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import type { KeyMetadataRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/keyMetadataRepresentation";
import { useAdminClient } from "../../admin-client";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";

export type RealmKeys = {
  realmKeys: KeyMetadataRepresentation[];
  canViewRealmKeys: boolean;
};

// Fetches the realm's key metadata for the signing-key selectors. When several selectors
// share one parent (the OIDC advanced tab renders four), the parent fetches once with this
// hook and passes the result down, so pass `enabled = false` in a selector that already
// received the keys to skip the duplicate request.
export const useRealmKeys = (enabled = true): RealmKeys => {
  const { adminClient } = useAdminClient();
  const { hasAccess } = useAccess();
  const { realm } = useRealm();
  const [realmKeys, setRealmKeys] = useState<KeyMetadataRepresentation[]>([]);

  // The keys endpoint is realm-scoped, so an admin who can manage a client but not the
  // realm cannot list its keys; in that case the selector degrades to read-only.
  const canViewRealmKeys = hasAccess("view-realm") || hasAccess("manage-realm");

  useFetch(
    async () => {
      if (!enabled || !canViewRealmKeys) {
        return [];
      }
      const keysMetadata = await adminClient.realms.getKeys({ realm });
      return keysMetadata.keys || [];
    },
    setRealmKeys,
    [enabled, canViewRealmKeys, realm],
  );

  return { realmKeys, canViewRealmKeys };
};
