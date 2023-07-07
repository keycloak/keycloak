import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { test as setup } from "@playwright/test";

import { importRealm } from "./admin-client";
import testRealm from "./test-realm.json" assert { type: "json" };

setup("import realm", () => importRealm(testRealm as RealmRepresentation));
