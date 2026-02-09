import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { clickAdd } from "../utils/modal.ts";
import { goToOrganizations, goToRealm } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickSelectRow,
  clickTableRowItem,
} from "../utils/table.ts";
import { clickAddRealmUser, goToMembersTab } from "./members.ts";

test.describe.serial("Members", () => {
  const realmName = `organization-members-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createOrganization({
      realm: realmName,
      name: "member",
      domains: [{ name: "o.com", verified: false }],
    });
    await adminClient.createUser({
      realm: realmName,
      username: "realm-user",
      enabled: true,
    });
  });
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, "member");
    await goToMembersTab(page);
  });

  test("should add member", async ({ page }) => {
    await clickAddRealmUser(page);
    await clickSelectRow(page, "Users", "realm-user");
    await clickAdd(page);
    await assertNotificationMessage(page, "1 user added to the organization");
    await assertRowExists(page, "realm-user");
  });
});
