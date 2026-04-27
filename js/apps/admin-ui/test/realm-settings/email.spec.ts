import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  clickSaveEmailButton,
  goToEmailTab,
  populateEmailPageNoAuth,
  assertEmailPageNoAuth,
  populateEmailPageWithPasswordAuth,
  assertEmailPageWithPasswordAuth,
  populateEmailPageWithTokenAuth,
  assertEmailPageWithTokenAuth,
} from "./email.ts";

test.describe.serial("Email", () => {
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

    await populateEmailPageWithPasswordAuth(page);
    await clickSaveEmailButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await goToRealmSettings(page);
    await goToEmailTab(page);
    await assertEmailPageWithPasswordAuth(page);

    await populateEmailPageWithTokenAuth(page);
    await clickSaveEmailButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await goToRealmSettings(page);
    await goToEmailTab(page);
    await assertEmailPageWithTokenAuth(page);
  });
});
