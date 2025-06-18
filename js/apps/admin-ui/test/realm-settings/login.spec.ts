import { Page, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { assertSwitchIsChecked, switchToggle } from "../utils/form";
import { login } from "../utils/login";
import {
  goToClientScopes,
  goToRealm,
  goToRealmSettings,
} from "../utils/sidebar";
import { goToLoginTab } from "./login";

test.describe("Realm settings tabs tests", () => {
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
    await switchToggle(page, `[data-testid="${realmSwitch}"]`);

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
});
