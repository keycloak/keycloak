import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { expect, test } from "@playwright/test";
import { toRealmSettings } from "../../src/realm-settings/routes/RealmSettings.tsx";
import { toAddUser } from "../../src/user/routes/AddUser.tsx";
import { toUser } from "../../src/user/routes/User.tsx";
import { toUsers } from "../../src/user/routes/Users.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import {
  assertAttributeLength,
  clickAttributeSaveButton,
  fillAttributeData,
  goToAttributesTab,
} from "../utils/attributes.ts";
import { DEFAULT_REALM } from "../utils/constants.ts";
import { selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import {
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToUsers } from "../utils/sidebar.ts";
import {
  assertNoResults,
  assertRowExists,
  clickTableRowItem,
  searchItem,
} from "../utils/table.ts";
import {
  clickAddUserButton,
  clickCancelButton,
  clickSaveButton,
  fillUserForm,
  joinGroup,
} from "./main.ts";

test.describe("User creation", () => {
  test("navigates to the create user page", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toUsers({ realm: testBed.realm }) });

    await clickAddUserButton(page);
    await expect(page).toHaveURL(/.*users\/add-user/);

    await clickCancelButton(page);
    await expect(page).not.toHaveURL(/.*users\/add-user/);
  });

  test("creates a new user", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toAddUser({ realm: testBed.realm }) });

    await fillUserForm(page, {
      username: "test-user",
      email: "test-user@example.com",
    });
    await clickSaveButton(page);
    await assertNotificationMessage(page, "The user has been created");
  });

  test("checks temporary admin user existence", async ({ page }) => {
    await login(page, { to: toUsers({ realm: DEFAULT_REALM }) });

    // check banner visibility first
    await expect(page.locator(".pf-v5-c-banner")).toContainText(
      "You are logged in as a temporary admin user.",
    );

    await searchItem(page, "Search", "admin");
    await assertRowExists(page, "admin");
    await expect(page.locator("#temporary-admin-label")).toBeVisible();
  });

  test("creates a user that joins a group", async ({ page }) => {
    await using testBed = await createTestBed({
      groups: [{ name: "test-group" }],
    });

    await login(page, { to: toAddUser({ realm: testBed.realm }) });

    await fillUserForm(page, { username: "test-user" });
    await joinGroup(page, ["test-group"]);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "The user has been created");
  });

  test("creates a user with a password credential", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toAddUser({ realm: testBed.realm }) });

    await fillUserForm(page, {
      username: "test-user",
      email: "test-user@example.com",
      firstName: "firstname",
      lastName: "lastname",
    });
    await clickSaveButton(page);
    await assertNotificationMessage(page, "The user has been created");

    await page.getByTestId("credentials").click();
    await page.getByTestId("no-credentials-empty-action").click();
    await page.getByTestId("passwordField").fill("test");
    await page.getByTestId("passwordConfirmationField").fill("test");

    await confirmModal(page);
    await confirmModal(page);

    await selectActionToggleItem(page, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, "The user has been deleted");
  });
});

test.describe("Existing users", () => {
  const placeHolder = "Search user";
  const existingUserName = "existing-user";
  const overrides: RealmRepresentation = {
    users: [{ username: existingUserName }],
  };

  test("searches for an existing user", async ({ page }) => {
    await using testBed = await createTestBed(overrides);

    await login(page, { to: toUsers({ realm: testBed.realm }) });

    await searchItem(page, placeHolder, existingUserName);
    await assertRowExists(page, existingUserName);
  });

  test("searches for a non-existing user", async ({ page }) => {
    await using testBed = await createTestBed(overrides);

    await login(page, { to: toUsers({ realm: testBed.realm }) });

    await searchItem(page, "Search", "non-existing-user");
    await assertNoResults(page);
  });

  test("edits a user", async ({ page }) => {
    await using testBed = await createTestBed(overrides);
    const user = await adminClient.findUserByUsername(
      testBed.realm,
      existingUserName,
    );

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "settings" }),
    });

    await fillUserForm(page, {
      email: "test-user@example.com",
      firstName: "first",
      lastName: "last",
    });
    await clickSaveButton(page);
    await assertNotificationMessage(page, "The user has been saved");
  });

  const attributesName = "unmanagedAttributes";

  test("adds unmanaged attributes to a user", async ({ page }) => {
    await using testBed = await createTestBed(overrides);

    await login(page, { to: toRealmSettings({ realm: testBed.realm }) });

    await selectItem(page, "#unmanagedAttributePolicy", "Enabled");
    await page.getByTestId("realmSettingsGeneralTab-save").click();

    await goToUsers(page);
    await clickTableRowItem(page, existingUserName);
    await goToAttributesTab(page);

    await fillAttributeData(page, "key_test", "value_test", attributesName);
    await clickAttributeSaveButton(page);
    await assertNotificationMessage(page, "The user has been saved");

    await fillAttributeData(page, "LDAP_ID", "value_test", attributesName, 1);
    await fillAttributeData(
      page,
      "LDAP_ID",
      "another_value_test",
      attributesName,
      2,
    );
    await clickAttributeSaveButton(page);

    await expect(page.getByText("Update of read-only attribute")).toHaveCount(
      2,
    );
    await assertNotificationMessage(page, "The user has not been saved: ");
  });

  test("adds unmanaged attributes with multiple values to a user", async ({
    page,
  }) => {
    await using testBed = await createTestBed(overrides);

    await login(page, { to: toRealmSettings({ realm: testBed.realm }) });

    await selectItem(page, "#unmanagedAttributePolicy", "Enabled");
    await page.getByTestId("realmSettingsGeneralTab-save").click();
    await goToUsers(page);
    await clickTableRowItem(page, existingUserName);
    await goToAttributesTab(page);

    await fillAttributeData(page, "key-multiple", "value1", attributesName);
    await fillAttributeData(page, "key-multiple", "value2", attributesName, 1);
    await clickAttributeSaveButton(page);

    await assertNotificationMessage(page, "The user has been saved");
    await assertAttributeLength(page, 2, attributesName);
  });

  test("adds a user to a group", async ({ page }) => {
    await using testBed = await createTestBed({
      ...overrides,
      groups: [{ name: "test-group" }],
    });
    const user = await adminClient.findUserByUsername(
      testBed.realm,
      existingUserName,
    );

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "groups" }),
    });

    await joinGroup(page, ["test-group"], true);
    await assertNotificationMessage(page, "Added group membership");
    await assertRowExists(page, "test-group");
  });
});
