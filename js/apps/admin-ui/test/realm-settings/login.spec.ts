import { type Page, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertSwitchIsChecked } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import {
  goToClientScopes,
  goToRealm,
  goToRealmSettings,
} from "../utils/sidebar.ts";
import { goToLoginTab, toggleLoginSettingAndExpectSuccess } from "./login.ts";

test.describe.serial("Realm settings tabs tests", () => {
  const realmName = `realm-settings_${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToLoginTab(page);
  });

  const testToggle = async (
    page: Page,
    realmSwitch: string,
    expectedValue: boolean,
  ) => {
    await toggleLoginSettingAndExpectSuccess(page, realmSwitch);

    await goToClientScopes(page);
    await goToRealmSettings(page);
    await goToLoginTab(page);

    await assertSwitchIsChecked(
      page,
      `[data-testid="${realmSwitch}"]`,
      !expectedValue,
    );
  };

  test("Go to login tab", async ({ page }) => {
    await testToggle(page, "user-reg-switch", true);
    await testToggle(page, "forgot-pw-switch", true);
    await testToggle(page, "remember-me-switch", true);
    await testToggle(page, "login-with-email-switch", false);
    await testToggle(page, "duplicate-emails-switch", true);

    await assertSwitchIsChecked(
      page,
      `[data-testid="email-as-username-switch"]`,
      true,
    );
  });

  test("Toggle passkeys enabled switch", async ({ page }) => {
    await testToggle(page, "passkeys-enabled-switch", true);
  });
});
