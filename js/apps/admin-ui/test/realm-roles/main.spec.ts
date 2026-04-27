import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { fillRoleData } from "../clients/role.ts";
import adminClient from "../utils/AdminClient.ts";
import {
  assertAttribute,
  assertAttributeLength,
  clickAttributeSaveButton,
  deleteAttribute,
  fillAttributeData,
  goToAttributesTab,
} from "../utils/attributes.ts";
import { assertRequiredFieldError, clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import {
  pickRoleType,
  clickUnassign,
  confirmModalAssign,
  pickRole,
} from "../utils/roles.ts";
import { goToRealm, goToRealmRoles } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertNoResults,
  assertRowExists,
  clickRowKebabItem,
  clickTableRowItem,
  searchItem,
} from "../utils/table.ts";
import {
  assertUnassignDisabled,
  clickCreateRoleButton,
  goToAssociatedRolesTab,
} from "./main.ts";

test.describe.serial("Realm roles test", () => {
  const realmName = `realm-roles-${uuid()}`;
  const prefix = "realm_role_crud";
  const searchPlaceHolder = "Search role by name";

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmRoles(page);
  });

  test("should fail creating realm role", async ({ page }) => {
    await clickCreateRoleButton(page);
    await clickSaveButton(page);
    await assertRequiredFieldError(page, "name");

    await fillRoleData(page, "admin");
    await clickSaveButton(page);
    await goToRealmRoles(page);

    await clickCreateRoleButton(page);
    await fillRoleData(page, "admin");
    await clickSaveButton(page);
    await assertNotificationMessage(
      page,
      "Could not create role: Role with name admin already exists",
    );
  });

  test("shouldn't create a realm role based with only whitespace name", async ({
    page,
  }) => {
    await clickCreateRoleButton(page);
    await fillRoleData(page, " ");
    await assertRequiredFieldError(page, "name");
  });

  test("Realm role CRUD test", async ({ page }) => {
    const itemId = prefix + uuid();

    // Create
    await assertRowExists(page, itemId, false);
    await clickCreateRoleButton(page);
    await fillRoleData(page, itemId);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Role created");
    await goToRealmRoles(page);

    await searchItem(page, searchPlaceHolder, itemId);
    await clickRowKebabItem(page, itemId, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, "The role has been deleted");

    await assertRowExists(page, itemId, false);
  });

  test("should delete role from details action", async ({ page }) => {
    const itemId = prefix + uuid();

    await clickCreateRoleButton(page);
    await fillRoleData(page, itemId);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Role created");
    await goToRealmRoles(page);
    await clickRowKebabItem(page, itemId, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, "The role has been deleted");
  });

  test("should not be able to delete default role", async ({ page }) => {
    const defaultRole = "default-roles-" + realmName;
    await searchItem(page, searchPlaceHolder, defaultRole);
    await clickRowKebabItem(page, defaultRole, "Delete");
    await assertNotificationMessage(page, "You cannot delete a default role.");
  });

  test("Add associated roles test", async ({ page }) => {
    const itemId = prefix + uuid();

    // Create
    await assertRowExists(page, itemId, false);
    await clickCreateRoleButton(page);
    await fillRoleData(page, itemId);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Role created");
    await goToAssociatedRolesTab(page);

    // Add associated realm role from search bar
    await pickRoleType(page, "roles");
    await pickRole(page, "offline_access", true);
    await confirmModalAssign(page);
    await assertNotificationMessage(page, "Associated roles have been added");

    // Add associated client role from search bar
    await pickRoleType(page, "client");
    await pickRole(page, "manage-account", true);
    await confirmModalAssign(page);
    await assertNotificationMessage(page, "Associated roles have been added");

    // Add associated client role
    await pickRoleType(page, "client");
    await pickRole(page, "manage-consent", true);
    await confirmModalAssign(page);
    await assertNotificationMessage(page, "Associated roles have been added");
  });

  test("should search existing associated role by name and go to it", async ({
    page,
  }) => {
    const realmRole = "offline_access";
    await searchItem(page, searchPlaceHolder, "offline_access");
    await assertRowExists(page, realmRole);
    await clickTableRowItem(page, realmRole);
    await expect(
      page.getByTestId("view-header").locator("text=" + realmRole),
    ).toBeVisible();
    await page.click("text=Cancel");
  });

  test("should go to default-roles-master link role name and check assign roles table is not empty", async ({
    page,
  }) => {
    const defaultRole = "default-roles-" + realmName;
    await clickTableRowItem(page, defaultRole);

    await page.click("text=Default groups");
    await assertEmptyTable(page);

    await page.click("text=Default roles");
    await expect(page.getByTestId("assigned-roles")).toBeVisible();
  });

  test("Should search non-existent associated role by name", async ({
    page,
  }) => {
    const itemName = "non-existent-associated-role";
    await searchItem(page, searchPlaceHolder, itemName);
    await assertNoResults(page);
  });

  test("Should hide inherited roles test", async ({ page }) => {
    const itemId = prefix + uuid();
    await adminClient.createRealmRole({ name: itemId, realm: realmName });

    await searchItem(page, searchPlaceHolder, itemId);
    await clickTableRowItem(page, itemId);
    await goToAssociatedRolesTab(page);
    await page.getByTestId("show-inherited-roles-empty-action").click();
  });

  test("Should fail to remove role when all unchecked from search bar", async ({
    page,
  }) => {
    const itemId = prefix + uuid();
    await adminClient.createRealmRole({ name: itemId, realm: realmName });

    await searchItem(page, searchPlaceHolder, itemId);
    await clickTableRowItem(page, itemId);
    await goToAssociatedRolesTab(page);

    await pickRoleType(page, "client");
    await pickRole(page, "view-profile", true);
    await confirmModalAssign(page);

    await assertUnassignDisabled(page);
  });

  test("Should delete single non-inherited role item", async ({ page }) => {
    const itemId = prefix + uuid();
    await adminClient.createRealmRole({ name: itemId, realm: realmName });

    await searchItem(page, searchPlaceHolder, itemId);
    await clickTableRowItem(page, itemId);
    await goToAssociatedRolesTab(page);

    await pickRoleType(page, "client");
    await pickRole(page, "view-profile", true);
    await confirmModalAssign(page);

    await clickRowKebabItem(page, "account view-profile", "Unassign");
    await confirmModal(page);
    await assertNotificationMessage(page, "Role mapping updated");
  });

  test("Should delete all roles from search bar", async ({ page }) => {
    const itemId = prefix + uuid();
    await adminClient.createRealmRole({ name: itemId, realm: realmName });

    await searchItem(page, searchPlaceHolder, itemId);
    await clickTableRowItem(page, itemId);
    await goToAssociatedRolesTab(page);

    await pickRoleType(page, "client");
    await pickRole(page, "view-profile", true);
    await confirmModalAssign(page);

    await page.locator('input[name="check-all"]').check();
    await clickUnassign(page);
    await confirmModal(page);
    await assertNotificationMessage(page, "Role mapping updated");
  });

  test.describe.serial("edit role details", () => {
    const editRoleName = "going to edit";
    const description = "some description";
    const updateDescription = "updated description";

    test.beforeEach(async () => {
      await adminClient.createRealmRole({
        realm: realmName,
        name: editRoleName,
        description,
      });
    });

    test.afterEach(() => adminClient.deleteRealmRole(editRoleName, realmName));

    test("should edit realm role details", async ({ page }) => {
      await searchItem(page, searchPlaceHolder, editRoleName);
      await clickTableRowItem(page, editRoleName);
      await expect(page.locator("input[name='name']")).toBeDisabled();
      await expect(page.locator("textarea[name='description']")).toHaveValue(
        description,
      );
      await page.fill("textarea[name='description']", updateDescription);
      await clickSaveButton(page);
      await assertNotificationMessage(page, "The role has been saved");
      await expect(page.locator("textarea[name='description']")).toHaveValue(
        updateDescription,
      );
    });

    test("should add attribute", async ({ page }) => {
      await searchItem(page, searchPlaceHolder, editRoleName);
      await clickTableRowItem(page, editRoleName);

      await goToAttributesTab(page);
      await fillAttributeData(page, "one", "1");
      await clickAttributeSaveButton(page);
      await assertNotificationMessage(page, "The role has been saved");
      await assertAttributeLength(page, 1);
    });

    test("should add attribute multiple", async ({ page }) => {
      await searchItem(page, searchPlaceHolder, editRoleName);
      await clickTableRowItem(page, editRoleName);

      await goToAttributesTab(page);
      await fillAttributeData(page, "one", "1");
      await fillAttributeData(page, "two", "2", undefined, 1);
      await fillAttributeData(page, "three", "3", undefined, 2);

      await clickAttributeSaveButton(page);
      await assertNotificationMessage(page, "The role has been saved");
      await assertAttributeLength(page, 3);
      await assertAttribute(page, "one", "1");
      await assertAttribute(page, "two", "2", 1);
    });

    test("should delete attribute", async ({ page }) => {
      await searchItem(page, searchPlaceHolder, editRoleName);
      await clickTableRowItem(page, editRoleName);
      await goToAttributesTab(page);
      await fillAttributeData(page, "one", "1");
      await fillAttributeData(page, "two", "2", undefined, 1);
      await clickAttributeSaveButton(page);

      await deleteAttribute(page, 1);
      await clickAttributeSaveButton(page);

      await assertNotificationMessage(page, "The role has been saved");
      await assertAttributeLength(page, 1);
    });
  });
});
