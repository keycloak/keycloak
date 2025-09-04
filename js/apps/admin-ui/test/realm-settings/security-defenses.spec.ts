import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  clickSaveBruteForce,
  clickSaveSecurityDefenses,
  fillMaxDeltaTimeSeconds,
  fillMaxFailureWaitSeconds,
  fillMinimumQuickLoginWaitSeconds,
  fillWaitIncrementSeconds,
  goToSecurityDefensesTab,
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
    await page.getByTestId("browserSecurityHeaders.xFrameOptions").fill("DENY");
    await clickSaveSecurityDefenses(page);
    await assertNotificationMessage(page, "Realm successfully updated");
  });

  test("Brute force detection", async ({ page }) => {
    await page.getByTestId("security-defenses-brute-force-tab").click();
    await selectItem(page, "#kc-brute-force-mode", "Lockout temporarily");
    await fillWaitIncrementSeconds(page, "1");
    await fillMaxFailureWaitSeconds(page, "1");
    await fillMaxDeltaTimeSeconds(page, "1");
    await fillMinimumQuickLoginWaitSeconds(page, "1");
    await clickSaveBruteForce(page);
    await assertNotificationMessage(page, "Realm successfully updated");
  });
});
