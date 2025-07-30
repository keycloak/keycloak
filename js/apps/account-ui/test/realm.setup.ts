import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { test as setup } from "@playwright/test";

import { deleteRealm, importRealm } from "./admin-client";
import groupsRealm from "./realms/groups-realm.json" assert { type: "json" };
import resourcesRealm from "./realms/resources-realm.json" assert { type: "json" };
import userProfileRealm from "./realms/user-profile-realm.json" assert { type: "json" };
import verifiableCredentialsRealm from "./realms/verifiable-credentials-realm.json" assert { type: "json" };

setup("import realm", async () => {
  await deleteRealm("photoz");
  await deleteRealm("groups");
  await deleteRealm("user-profile");
  await deleteRealm("verifiable-credentials");
  await importRealm(groupsRealm as RealmRepresentation);
  await importRealm(resourcesRealm as RealmRepresentation);
  await importRealm(userProfileRealm as RealmRepresentation);
  await importRealm(verifiableCredentialsRealm as RealmRepresentation);
});
