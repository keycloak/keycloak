import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  assertAccessTokenLifespan,
  assertAccessTokenLifespanImplicitInput,
  assertParRequestUriLifespan,
  clickSaveSessionsButton,
  goToTokensTab,
  populateTokensPage,
} from "./tokens.ts";

test.describe.serial("Realm Settings - Tokens", () => {
  const realmName = `tokens-realm-settings-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToTokensTab(page);
  });

  test("Add token data", async ({ page }) => {
    await populateTokensPage(page);
    await clickSaveSessionsButton(page);
    await assertNotificationMessage(page, "Realm successfully updated");

    await assertAccessTokenLifespan(page, "1");
    await assertParRequestUriLifespan(page, "2");
    await assertAccessTokenLifespanImplicitInput(page, "2");
  });
});
