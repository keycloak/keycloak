import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { test as setup } from "@playwright/test";

import { deleteRealm, importRealm } from "./admin-client";
import groupsRealm from "./realms/groups-realm.json" assert { type: "json" };
import resourcesRealm from "./realms/resources-realm.json" assert { type: "json" };
import userProfileRealm from "./realms/user-profile-realm.json" assert { type: "json" };
import verifiableCredentialsRealm from "./realms/verifiable-credentials-realm.json" assert { type: "json" };

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
