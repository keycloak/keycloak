import { test } from "@playwright/test";
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
  populateSessionsPage,
} from "./sessions.ts";

test.describe.serial("Sessions", () => {
  const realmName = `sessions-realm-settings-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToSessionsTab(page);
  });

  test("Add session data", async ({ page }) => {
    await populateSessionsPage(page);
    await clickSaveSessionsButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");

    await assertSsoSessionIdleInput(page, "1");
    await assertSsoSessionMaxInput(page, "2");
    await assertSsoSessionIdleRememberMe(page, "3");
    await assertSsoSessionMaxRememberMe(page, "4");
  });
});
