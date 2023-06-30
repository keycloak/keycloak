import { test as setup } from "@playwright/test";
import testRealm from "./test-realm.json" assert { type: "json" };
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import adminClient from "./AdminClient";

setup("import realm", async () => {
  await adminClient.importRealm(testRealm as RealmRepresentation);
});
