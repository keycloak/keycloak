import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertSsoSessionIdleInput,
  assertSsoSessionIdleRememberMe,
  assertSsoSessionMaxInput,
  assertSsoSessionMaxRememberMe,
  clickSaveSessionsButton,
  goToSessionsTab,
  populateSessionsPageRememberMeDisabled,
  populateSessionsPageRememberMeEnabled,
} from "./sessions.ts";

test.describe.serial("Sessions", () => {
  const realmName = `sessions-realm-settings-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
  });

  test("Add session data when Remember Me is disabled", async ({ page }) => {
    //Disable Remember Me and save realm
    await page.getByTestId("rs-login-tab").click();
    await page.getByLabel("Remember Me").uncheck();
    await goToSessionsTab(page);
    // verify remember me fields are not visible
    await expect(
      page.getByTestId("sso-session-idle-remember-me-input"),
    ).toHaveCount(0);
    await expect(
      page.getByTestId("sso-session-max-remember-me-input"),
    ).toHaveCount(0);
    await populateSessionsPageRememberMeDisabled(page);
    await clickSaveSessionsButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await assertSsoSessionIdleInput(page, "5");
    await assertSsoSessionMaxInput(page, "2");
  });
  test("Add session data when Remember Me is enabled", async ({ page }) => {
    //Enable Remember Me and save realm
    await page.getByTestId("rs-login-tab").click();
    const rememberMeSwitch = page.locator('[for="kc-remember-me-switch"]');
    await rememberMeSwitch.click();
    await goToSessionsTab(page);
    await populateSessionsPageRememberMeEnabled(page);
    await clickSaveSessionsButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");
    await assertSsoSessionIdleInput(page, "5");
    await assertSsoSessionMaxInput(page, "2");
    await assertSsoSessionIdleRememberMe(page, "3");
    await assertSsoSessionMaxRememberMe(page, "4");
  });
});
