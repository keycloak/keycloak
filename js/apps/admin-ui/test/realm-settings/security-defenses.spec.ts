import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  clickSaveBruteForce,
  fillXFrameOptionsSecurityHeader,
  assertXFrameOptionsSecurityHeaderValue,
  clickSaveSecurityDefenses,
  selectBruteForceMode,
  fillMaxDeltaTimeSeconds,
  fillMaxFailureWaitSeconds,
  fillMinimumQuickLoginWaitSeconds,
  fillWaitIncrementSeconds,
  goToSecurityDefensesTab,
  goToBruteForceTab,
} from "./security-defenses.ts";

test.describe.serial("Security defenses", () => {
  const realmName = `security-defenses-realm-settings-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToSecurityDefensesTab(page);
  });

  test("Realm header settings", async ({ page }) => {
    await fillXFrameOptionsSecurityHeader(page, "DENY");
    await clickSaveSecurityDefenses(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await assertXFrameOptionsSecurityHeaderValue(page, "DENY");
  });

  test("Brute force detection", async ({ page }) => {
    await goToBruteForceTab(page);
    await selectBruteForceMode(page, "Lockout temporarily");
    await fillWaitIncrementSeconds(page, "1");
    await fillMaxFailureWaitSeconds(page, "1");
    await fillMaxDeltaTimeSeconds(page, "1");
    await fillMinimumQuickLoginWaitSeconds(page, "1");
    await clickSaveBruteForce(page);
    await assertNotificationMessage(page, "Realm successfully updated");
  });

  test("Realm header settings followed by Brute force detection", async ({
    page,
  }) => {
    await fillXFrameOptionsSecurityHeader(page, "ALLOW-FROM foo");
    await clickSaveSecurityDefenses(page);
    await assertNotificationMessage(page, "Realm successfully updated");

    await goToBruteForceTab(page);
    await selectBruteForceMode(page, "Lockout temporarily");
    await fillWaitIncrementSeconds(page, "2");
    await clickSaveBruteForce(page);
    await assertNotificationMessage(page, "Realm successfully updated");

    await goToSecurityDefensesTab(page);
    await assertXFrameOptionsSecurityHeaderValue(page, "ALLOW-FROM foo");
  });
});
