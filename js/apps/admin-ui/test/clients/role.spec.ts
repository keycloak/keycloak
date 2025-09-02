import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import {
  assertAttributeLength,
  clickAttributeSaveButton,
  deleteAttribute,
  fillAttributeData,
  goToAttributesTab,
} from "../utils/attributes.ts";
import { assertRequiredFieldError, clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import {
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { assertModalTitle, confirmModal } from "../utils/modal.ts";
import { clickUnassign } from "../utils/roles.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import {
  assertNoResults,
  assertRowExists,
  clearAllFilters,
  clickRowKebabItem,
  clickSelectRow,
  clickTableRowItem,
  searchItem,
} from "../utils/table.ts";
import {
  addAssociatedRoles,
  assertDescriptionValue,
  fillRoleData,
  goToAssociatedRolesTab,
  goToCreateRole,
  goToCreateRoleFromEmptyState,
  goToRolesTab,
} from "./role.ts";

test.describe.serial("Roles tab test", () => {
  const realmName = `clients-realm-${uuid()}`;
  const itemId = `client-crud-${uuid()}`;
  const updatableItem = `role-name-${uuid()}`;
  const client = `client-${uuid()}`;
  const oneRoleClient = `client-one-role-${uuid()}`;
  const createRealmRoleName = `create-realm-${uuid()}`;
  const placeHolder = "Search role by name";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createClient({
      clientId: client,
      protocol: "openid-connect",
      publicClient: false,
      realm: realmName,
    });
    await adminClient.createRealmRole({
      name: createRealmRoleName,
      realm: realmName,
    });
    const { id } = await adminClient.createClient({
      clientId: oneRoleClient,
      protocol: "openid-connect",
      publicClient: false,
      realm: realmName,
    });
    await adminClient.createClientRole(id, {
      name: updatableItem,
      realm: realmName,
    });
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test("should fail to create client role with empty name", async ({
    page,
  }) => {
    await clickTableRowItem(page, client);
    await goToRolesTab(page);

    await goToCreateRoleFromEmptyState(page);
    await fillRoleData(page, "");
    await clickSaveButton(page);
    await assertRequiredFieldError(page, "name");
  });

  test("should create client role", async ({ page }) => {
    await clickTableRowItem(page, client);
    await goToRolesTab(page);

    await goToCreateRoleFromEmptyState(page);
    await fillRoleData(page, itemId);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Role created");
  });

  test("should update client role description", async ({ page }) => {
    await clickTableRowItem(page, oneRoleClient);
    await goToRolesTab(page);

    const updateDescription = "updated description";
    await clickTableRowItem(page, updatableItem);
    await fillRoleData(page, updatableItem, updateDescription);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "The role has been saved");
    await assertDescriptionValue(page, updateDescription);
  });

  test("should add and delete attribute to client role", async ({ page }) => {
    await clickTableRowItem(page, oneRoleClient);
    await goToRolesTab(page);
    await clickTableRowItem(page, updatableItem);
    await goToAttributesTab(page);

    // Add attribute
    await fillAttributeData(page, "crud_attribute_key", "crud_attribute_value");
    await clickAttributeSaveButton(page);
    await assertAttributeLength(page, 1);
    await assertNotificationMessage(page, "The role has been saved");

    // Delete attribute
    await deleteAttribute(page, 0);
    await clickAttributeSaveButton(page);
    await assertNotificationMessage(page, "The role has been saved");
    await assertAttributeLength(page, 0);
  });

  test("should fail to create duplicate client role", async ({ page }) => {
    await clickTableRowItem(page, oneRoleClient);
    await goToRolesTab(page);

    await goToCreateRole(page);
    await fillRoleData(page, updatableItem);
    await clickSaveButton(page);
    await assertNotificationMessage(
      page,
      `Could not create role: Role with name ${updatableItem} already exists`,
    );
  });

  test("should search existing and non-existing client role", async ({
    page,
  }) => {
    await clickTableRowItem(page, oneRoleClient);
    await goToRolesTab(page);

    // Search non-existing
    await searchItem(page, placeHolder, "role_DNE");
    await assertNoResults(page);

    // Search existing
    await clearAllFilters(page);
    await searchItem(page, placeHolder, updatableItem);
    await assertRowExists(page, updatableItem);

    // Empty search
    await searchItem(page, placeHolder, "");
    await assertRowExists(page, updatableItem);
  });

  test("should handle associated realm roles", async ({ page }) => {
    await clickTableRowItem(page, oneRoleClient);
    await goToRolesTab(page);
    await clickTableRowItem(page, updatableItem);
    await goToAssociatedRolesTab(page);

    // Add associated realm role
    await addAssociatedRoles(page, createRealmRoleName);
    await assertNotificationMessage(page, "Associated roles have been added");

    // Remove associated roles
    await clickSelectRow(page, "Role list", createRealmRoleName);
    await clickUnassign(page);
    await confirmModal(page);
    await assertNotificationMessage(page, "Role mapping updated");
  });

  test("should handle associated client roles", async ({ page }) => {
    await clickTableRowItem(page, oneRoleClient);
    await goToRolesTab(page);
    await clickTableRowItem(page, updatableItem);
    await goToAssociatedRolesTab(page);

    // Add associated client roles
    await addAssociatedRoles(page, "manage-account", "client");
    await assertNotificationMessage(page, "Associated roles have been added");
  });

  test("should delete client role", async ({ page }) => {
    // Delete from list
    await clickTableRowItem(page, oneRoleClient);
    await goToRolesTab(page);

    await clickRowKebabItem(page, updatableItem, "Delete");
    await assertModalTitle(page, "Delete role?");
    await confirmModal(page);
  });

  test.skip("Should delete client role from role details test", async ({
    page,
  }) => {
    await clickTableRowItem(page, oneRoleClient);
    await goToRolesTab(page);
    await clickTableRowItem(page, updatableItem);

    await selectActionToggleItem(page, "Delete this role");
    await confirmModal(page);
    await assertNotificationMessage(page, "The role has been deleted");
  });
});
