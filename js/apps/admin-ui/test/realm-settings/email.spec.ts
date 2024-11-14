import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { goToRealm, goToRealmSettings } from "../utils/sidebar";
import {
  clickSaveEmailButton,
  goToEmailTab,
  populateEmailPageNoAuth,
  assertEmailPageNoAuth,
  populateEmailPageWithPasswordAuth,
  assertEmailPageWithPasswordAuth,
  populateEmailPageWithTokenAuth,
  assertEmailPageWithTokenAuth,
} from "./email";

test.describe("Email", () => {
  const realmName = `email-realm-settings-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToEmailTab(page);
  });

  test("Add email data with no authentication", async ({ page }) => {
    await populateEmailPageNoAuth(page);
    await clickSaveEmailButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await goToRealmSettings(page);
    await goToEmailTab(page);
    await assertEmailPageNoAuth(page);
  });

  test("Add email data with password authentication", async ({ page }) => {
    await populateEmailPageWithPasswordAuth(page);
    await clickSaveEmailButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await goToRealmSettings(page);
    await goToEmailTab(page);
    await assertEmailPageWithPasswordAuth(page);
  });

  test("Add email data with token authentication", async ({ page }) => {
    await populateEmailPageWithTokenAuth(page);
    await clickSaveEmailButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await goToRealmSettings(page);
    await goToEmailTab(page);
    await assertEmailPageWithTokenAuth(page);
  });
});
