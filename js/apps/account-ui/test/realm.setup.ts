import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { test as setup } from "@playwright/test";

import { deleteRealm, importRealm } from "./admin-client.ts";
import groupsRealm from "./realms/groups-realm.json" with { type: "json" };
import resourcesRealm from "./realms/resources-realm.json" with { type: "json" };
import userProfileRealm from "./realms/user-profile-realm.json" with { type: "json" };
import verifiableCredentialsRealm from "./realms/verifiable-credentials-realm.json" with { type: "json" };

setup("import realm", async () => {
  await Promise.allSettled([
    deleteRealm(groupsRealm.realm),
    deleteRealm(resourcesRealm.realm),
    deleteRealm(userProfileRealm.realm),
    deleteRealm(verifiableCredentialsRealm.realm),
  ]);
  await Promise.all([
    importRealm(groupsRealm),
    importRealm(resourcesRealm as RealmRepresentation),
    importRealm(userProfileRealm),
    importRealm(verifiableCredentialsRealm as RealmRepresentation),
  ]);
});
