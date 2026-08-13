import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import {
  pickRoleType,
  clickHideInheritedRoles,
  clickUnassign,
  confirmModalAssign,
  pickRole,
} from "../utils/roles.ts";
import { goToGroups } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertRowExists,
  clickTableRowItem,
} from "../utils/table.ts";
import { goToRoleMappingTab } from "./role.ts";

test.describe.serial("Role mappings", () => {
  const predefinedGroup = "group1";
  const predefinedGroup1 = "group2";

  const roleName = `remove-role-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createGroup(predefinedGroup);
    const { id } = await adminClient.createGroup(predefinedGroup1);

    await adminClient.createRealmRole({
      name: roleName,
      clientRole: true,
    });
    await adminClient.addRealmRoleToGroup(id, roleName);
  });

  test.afterAll(async () => {
    await adminClient.deleteRealmRole(roleName);
    await adminClient.deleteGroups();
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToGroups(page);
    await clickTableRowItem(page, predefinedGroup);
    await goToRoleMappingTab(page);
  });

  test("Check empty state", async ({ page }) => {
    await assertEmptyTable(page);
  });

  test("Assign roles from empty state", async ({ page }) => {
    await pickRoleType(page, "roles");
    await pickRole(page, "default-roles-master", true);
    await confirmModalAssign(page);

    await assertNotificationMessage(page, "Role mapping updated");
    await assertRowExists(page, "default-roles-master");
  });

  test("Check hide inherited roles option", async ({ page }) => {
    await goToGroups(page);
    await clickTableRowItem(page, predefinedGroup1);
    await goToRoleMappingTab(page);

    await clickHideInheritedRoles(page);
  });

  test("Remove roles", async ({ page }) => {
    await goToGroups(page);
    await clickTableRowItem(page, predefinedGroup1);
    await goToRoleMappingTab(page);

    await pickRole(page, roleName);
    await clickUnassign(page);
    await confirmModal(page);
    await assertNotificationMessage(page, "Role mapping updated");
    await assertEmptyTable(page);
  });
});
