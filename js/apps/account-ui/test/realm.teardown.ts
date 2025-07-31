import { test as setup } from "@playwright/test";
import { deleteRealm } from "./admin-client";
import groupsRealm from "./realms/groups-realm.json" assert { type: "json" };
import resourcesRealm from "./realms/resources-realm.json" assert { type: "json" };
import userProfileRealm from "./realms/user-profile-realm.json" assert { type: "json" };
import verifiableCredentialsRealm from "./realms/verifiable-credentials-realm.json" assert { type: "json" };

setup("delete realm", async () => {
  await Promise.all([
    deleteRealm(groupsRealm.realm),
    deleteRealm(resourcesRealm.realm),
    deleteRealm(userProfileRealm.realm),
    deleteRealm(verifiableCredentialsRealm.realm),
  ]);
});
