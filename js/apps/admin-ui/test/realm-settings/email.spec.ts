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
  assertTestConnectionHint,
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

test.describe("Email test connection hint", () => {
  const realmName = `email-hint-realm-settings-${uuid()}`;
  const username = `email-hint-admin-${uuid()}`;
  const email = `${username}@example.com`;
  const password = "password";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    const user = await adminClient.createUser({
      username,
      email,
      enabled: true,
      credentials: [{ type: "password", value: password }],
    });
    await adminClient.addRealmRoleToUser(user.id!, "admin");
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
    await adminClient.deleteUser(username);
  });

  test("Shows the email of a master admin in a non-master realm", async ({
    page,
  }) => {
    await login(page, { username, password });
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToEmailTab(page);
    await assertTestConnectionHint(page, email);
  });
});
