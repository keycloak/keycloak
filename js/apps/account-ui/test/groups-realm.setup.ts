import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { test as setup } from "@playwright/test";

import { importRealm } from "./admin-client";
import groupsRealm from "./groups-realm.json" assert { type: "json" };

setup("import realm", async () => {
  await importRealm(groupsRealm as RealmRepresentation);
});
