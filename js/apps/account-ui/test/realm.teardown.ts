import { test as setup } from "@playwright/test";
import { deleteRealm } from "./admin-client";
import groupsRealm from "./realms/groups-realm.json";
import resourcesRealm from "./realms/resources-realm.json";
import userProfileRealm from "./realms/user-profile-realm.json";
import verifiableCredentialsRealm from "./realms/verifiable-credentials-realm.json";

setup("delete realm", async () => {
  await Promise.all([
    deleteRealm(groupsRealm.realm),
    deleteRealm(resourcesRealm.realm),
    deleteRealm(userProfileRealm.realm),
    deleteRealm(verifiableCredentialsRealm.realm),
  ]);
});
