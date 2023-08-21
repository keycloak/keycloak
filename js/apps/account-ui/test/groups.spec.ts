import { test, expect } from "@playwright/test";
import { login } from "./login";
import { deleteRealm, importRealm } from "./admin-client";
import groupsRealm from "./groups-realm.json" assert { type: "json" };
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

test.describe("Groups page", () => {
  test.beforeAll(() => importRealm(groupsRealm as RealmRepresentation));
  test.afterAll(() => deleteRealm("groups"));

  test("List my groups", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "groups");
    await page.getByTestId("groups").click();
    await expect(page.getByTestId("group[0].name")).toHaveText("one");
  });
});
