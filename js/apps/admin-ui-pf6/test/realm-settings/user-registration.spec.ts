import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { pickRoleType, confirmModalAssign, pickRole } from "../utils/roles.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  searchItem,
} from "../utils/table.ts";
import {
  goToDefaultGroupTab,
  goToUserRegistrationTab,
} from "./user-registration.ts";

const groupName = "The default group";

test.describe.serial("Realm settings - User registration tab", () => {
  const realmName = `realm-settings-user-registration-${uuid()}`;
  const roleName = "theRole";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createRealmRole({ name: roleName, realm: realmName });
    await adminClient.createGroup(groupName, realmName);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToUserRegistrationTab(page);
  });

  test(`Add / remove ${roleName} role`, async ({ page }) => {
    const roleType = "roles";

    await pickRoleType(page, roleType);
    await pickRole(page, roleName, true);
    await confirmModalAssign(page);
    await assertNotificationMessage(page, "Associated roles have been added");

    await searchItem(page, "Search", roleName);
    await assertRowExists(page, roleName);

    // remove role

    await clickRowKebabItem(page, roleName, "Unassign");
    await confirmModal(page);
    await assertNotificationMessage(page, "Role mapping updated");
  });

  test("Add default group", async ({ page }) => {
    await goToDefaultGroupTab(page);
    await page.getByTestId("no-default-groups-empty-action").click();

    await page.getByTestId(`${groupName}-check`).click();
    await page.getByTestId("add-button").click();
    await assertNotificationMessage(
      page,
      "New group added to the default groups",
    );

    await assertRowExists(page, groupName);
  });
});
