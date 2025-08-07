import { test as setup } from "@playwright/test";
import { deleteRealm } from "./admin-client.ts";
import groupsRealm from "./realms/groups-realm.json" with { type: "json" };
import resourcesRealm from "./realms/resources-realm.json" with { type: "json" };
import userProfileRealm from "./realms/user-profile-realm.json" with { type: "json" };
import verifiableCredentialsRealm from "./realms/verifiable-credentials-realm.json" with { type: "json" };

setup("delete realm", async () => {
  await Promise.all([
    deleteRealm(groupsRealm.realm),
    deleteRealm(resourcesRealm.realm),
    deleteRealm(userProfileRealm.realm),
    deleteRealm(verifiableCredentialsRealm.realm),
  ]);
});
