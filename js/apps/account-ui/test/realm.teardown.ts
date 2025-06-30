import { test as setup } from "@playwright/test";
import { deleteRealm } from "./admin-client";

setup("delete realm", async () => {
  await deleteRealm("photoz");
  await deleteRealm("groups");
  await deleteRealm("user-profile");
  await deleteRealm("verifiable-credentials");
});
