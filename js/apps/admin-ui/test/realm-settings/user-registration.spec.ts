import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../../cypress/support/util/AdminClient";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { confirmModal } from "../utils/modal";
import {
  changeRoleTypeFilter,
  confirmModalAssign,
  pickRole,
} from "../utils/roles";
import { goToRealm, goToRealmRoles, goToRealmSettings } from "../utils/sidebar";
import {
  assertRowExists,
  clickRowKebabItem,
  clickTableRowItem,
  searchItem,
} from "../utils/table";
import {
  clickAssignRole,
  goToDefaultGroupTab,
  goToUserRegistrationTab,
} from "./user-registration";

const groupName = "The default group";

test.describe("Realm settings - User registration tab", () => {
  const realmName = `realm-settings-user-registration-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createGroup(groupName, realmName);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToUserRegistrationTab(page);
  });

  test("Add admin role", async ({ page }) => {
    const role = "admin";
    const roleType = "roles";

    await clickAssignRole(page);
    await changeRoleTypeFilter(page, roleType);
    await pickRole(page, role, true);
    await confirmModalAssign(page);
    await assertNotificationMessage(page, "Associated roles have been added");

    await searchItem(page, "Search", role);
    await assertRowExists(page, role);

    await goToRealmRoles(page);
    await clickTableRowItem(page, "admin");
    await page.getByTestId("usersInRoleTab").click();
    await expect(page.getByTestId("users-in-role-table")).toContainText(
      "admin",
    );
  });

  test("Remove admin role", async ({ page }) => {
    const role = "admin";

    await clickRowKebabItem(page, role, "Unassign");
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
