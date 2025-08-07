import { test as setup } from "@playwright/test";
import { deleteRealm } from "./admin-client.ts";

setup("delete realm", async () => {
  await deleteRealm("photoz");
  await deleteRealm("groups");
  await deleteRealm("user-profile");
  await deleteRealm("verifiable-credentials");
});
