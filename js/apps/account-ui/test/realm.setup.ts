import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { test as setup } from "@playwright/test";

import { importRealm } from "./admin-client.ts";
import groupsRealm from "./realms/groups-realm.json" with { type: "json" };
import resourcesRealm from "./realms/resources-realm.json" with { type: "json" };
import userProfileRealm from "./realms/user-profile-realm.json" with { type: "json" };
import verifiableCredentialsRealm from "./realms/verifiable-credentials-realm.json" with { type: "json" };

setup("import realm", async () => {
  await importRealm(groupsRealm as RealmRepresentation);
  await importRealm(resourcesRealm as RealmRepresentation);
  await importRealm(userProfileRealm as RealmRepresentation);
  await importRealm(verifiableCredentialsRealm as RealmRepresentation);
});
