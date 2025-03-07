import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import {
  assertAttributeLength,
  clickAttributeSaveButton,
  fillAttributeData,
  goToAttributesTab,
} from "../utils/attributes";
import { selectItem } from "../utils/form";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { confirmModal } from "../utils/modal";
import { goToRealm, goToRealmSettings, goToUsers } from "../utils/sidebar";
import {
  assertNoResults,
  assertRowExists,
  clickTableRowItem,
  searchItem,
} from "../utils/table";
import {
  clickAddUserButton,
  clickCancelButton,
  clickSaveButton,
  fillUserForm,
  goToGroupTab,
  joinGroup,
} from "./main";

let groupName = "group";
let groupsList: string[] = [];

test.describe("User creation", () => {
  const realmName = `users-${uuid()}`;

  const userId = `user_crud-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);

    for (let i = 0; i <= 2; i++) {
      groupName += "_" + uuid();
      await adminClient.createGroup(groupName, realmName);
      groupsList = [...groupsList, groupName];
    }
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await selectItem(page, "#unmanagedAttributePolicy", "Enabled");
    await page.getByTestId("realmSettingsGeneralTab-save").click();
    await goToUsers(page);
  });

  test.afterEach(() => adminClient.deleteUser(userId, realmName, true));

  test("Go to create User page", async ({ page }) => {
    await clickAddUserButton(page);
    await expect(page).toHaveURL(/.*users\/add-user/);

    await clickCancelButton(page);
    await expect(page).not.toHaveURL(/.*users\/add-user/);
  });

  test("Create user test", async ({ page }) => {
    await clickAddUserButton(page);
    await fillUserForm(page, {
      username: userId,
      email: `example_${uuid()}@example.com`,
    });
    await clickSaveButton(page);
    await assertNotificationMessage(page, "The user has been created");
  });

  test("Should check temporary admin user existence", async ({ page }) => {
    // check banner visibility first
    await expect(page.locator(".pf-v5-c-banner")).toContainText(
      "You are logged in as a temporary admin user.",
    );

    await goToRealm(page, "master");
    await goToUsers(page);
    await searchItem(page, "Search", "admin");
    await assertRowExists(page, "admin");
    await expect(page.locator("#temporary-admin-label")).toBeVisible();
  });

  test("Create user with groups test", async ({ page }) => {
    await clickAddUserButton(page);
    await fillUserForm(page, { username: userId });
    await joinGroup(page, [groupsList[0]]);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "The user has been created");
  });

  test("Create user with credentials test", async ({ page }) => {
    await clickAddUserButton(page);
    await fillUserForm(page, {
      username: userId,
      email: `example_${uuid()}@example.com`,
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
  });

  test.describe("Existing users", () => {
    const placeHolder = "Search user";
    const existingUserId = `existing_user-${uuid()}`;

    test.beforeAll(() =>
      adminClient.createUser({
        realm: realmName,
        username: existingUserId,
      }),
    );

    test("Search existing user test", async ({ page }) => {
      await searchItem(page, placeHolder, existingUserId);
      await assertRowExists(page, existingUserId);
    });

    test("Search non-existing user test", async ({ page }) => {
      await searchItem(page, "Search", "user_DNE");
      await assertNoResults(page);
    });

    test("User details test", async ({ page }) => {
      await searchItem(page, placeHolder, existingUserId);
      await assertRowExists(page, existingUserId);
      await clickTableRowItem(page, existingUserId);

      await fillUserForm(page, {
        email: `example_${uuid()}@example.com`,
        firstName: "first",
        lastName: "last",
      });
      await clickSaveButton(page);
      await assertNotificationMessage(page, "The user has been saved");

      await goToUsers(page);
      await searchItem(page, placeHolder, existingUserId);
      await assertRowExists(page, existingUserId);
    });

    const attributesName = "unmanagedAttributes";

    test("Select Unmanaged attributes", async ({ page }) => {
      await clickTableRowItem(page, existingUserId);

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

    test("User attributes with multiple values test", async ({ page }) => {
      await clickTableRowItem(page, existingUserId);

      await goToAttributesTab(page);
      await fillAttributeData(page, "key-multiple", "value1", attributesName);
      await fillAttributeData(
        page,
        "key-multiple",
        "value2",
        attributesName,
        1,
      );
      await clickAttributeSaveButton(page);
      await assertNotificationMessage(page, "The user has been saved");
      await assertAttributeLength(page, 2, attributesName);
    });

    test("Add user to groups test", async ({ page }) => {
      await clickTableRowItem(page, existingUserId);

      await goToGroupTab(page);
      await joinGroup(page, groupsList, true);
      await assertNotificationMessage(page, "Added group membership");
      await assertRowExists(page, groupsList[2]);
    });
  });
});
