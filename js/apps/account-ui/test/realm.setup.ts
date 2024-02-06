import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { test as setup } from "@playwright/test";

import { importRealm } from "./admin-client";
// @ts-ignore
import groupsRealm from "./realms/groups-realm.json" assert { type: "json" };
// @ts-ignore
import resourcesRealm from "./realms/resources-realm.json" assert { type: "json" };
// @ts-ignore
import userProfileRealm from "./realms/user-profile-realm.json" assert { type: "json" };

setup("import realm", async () => {
  await importRealm(groupsRealm as RealmRepresentation);
  await importRealm(resourcesRealm as RealmRepresentation);
  await importRealm(userProfileRealm as RealmRepresentation);
});
