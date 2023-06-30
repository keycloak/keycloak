import { test as setup } from "@playwright/test";
import adminClient from "./AdminClient";

setup("delete realm", async () => {
  await adminClient.deleteRealm("photoz");
});
