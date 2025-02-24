import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { assertRequiredFieldError, switchOff } from "../utils/form";
import { login } from "../utils/login";
import {
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead";
import { confirmModal } from "../utils/modal";
import { goToClients, goToRealmSettings } from "../utils/sidebar";
import { assertRowExists } from "../utils/table";
import {
  assertCurrentRealm,
  clickClearResourceFile,
  clickConfirmClear,
  clickCreateRealm,
  clickCreateRealmForm,
  fillRealmName,
  goToRealmSection,
} from "./realm";

const testRealmName = `Test-realm-${uuid()}`;
const newRealmName = `New-Test-realm-${uuid()}`;
const editedRealmName = `Edited-Test-realm-${uuid()}`;
const testDisabledName = `Test-Disabled-${uuid()}`;
const specialCharsName = `%22-${uuid()}`;

test.describe("Realm tests", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await clickCreateRealm(page);
  });

  test.afterAll(async () => {
    await Promise.all(
      [testRealmName, newRealmName, editedRealmName, specialCharsName].map(
        (realm) => adminClient.deleteRealm(realm),
      ),
    );
  });

  test("should fail creating duplicated or empty name realm", async ({
    page,
  }) => {
    await page.getByTestId("create").click();
    await assertRequiredFieldError(page, "realm");

    await fillRealmName(page, "master");
    await clickCreateRealmForm(page);

    await assertNotificationMessage(
      page,
      "Could not create realm Realm master already exists",
    );
  });

  test("should create Test realm", async ({ page }) => {
    await page.locator(".w-tc-editor-text").fill("clear this field");
    await clickClearResourceFile(page);
    await clickConfirmClear(page);

    await fillRealmName(page, testRealmName);
    await clickCreateRealmForm(page);

    await assertNotificationMessage(page, "Realm created successfully");
  });

  test("CRUD test of Disabled realm", async ({ page }) => {
    await fillRealmName(page, testDisabledName);
    await clickCreateRealmForm(page);

    await assertNotificationMessage(page, "Realm created successfully");

    await goToRealmSettings(page);

    await switchOff(page, `#${testDisabledName}-switch`);
    await confirmModal(page);

    await assertNotificationMessage(page, "Realm successfully updated");

    await goToRealmSettings(page);
    await selectActionToggleItem(page, "Delete");
    await confirmModal(page);

    await assertNotificationMessage(page, "The realm has been deleted");

    await assertCurrentRealm(page, testDisabledName, true);
  });

  test("should create realm from new a realm", async ({ page }) => {
    await fillRealmName(page, newRealmName);
    await clickCreateRealmForm(page);

    await assertNotificationMessage(page, "Realm created successfully");

    await clickCreateRealm(page);
    await fillRealmName(page, editedRealmName);
    await clickCreateRealmForm(page);

    await assertNotificationMessage(page, "Realm created successfully");

    await goToRealmSection(page);
    await assertRowExists(page, newRealmName);
    await assertRowExists(page, editedRealmName);
  });

  test("should create realm with special characters", async ({ page }) => {
    await fillRealmName(page, specialCharsName);
    await clickCreateRealmForm(page);

    await goToClients(page);
    await assertRowExists(page, "account-console");
  });
});
