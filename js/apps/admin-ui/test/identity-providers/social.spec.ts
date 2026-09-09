import { test, expect } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";
import { switchOff, switchOn } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickSaveButton } from "./main.ts";

const alias = "google-login-hint";

test.describe.serial("Social identity provider pass login_hint", () => {
  test.beforeEach(async ({ page }) => {
    try {
      await adminClient.deleteIdentityProvider(alias);
    } catch {
      // The provider may not exist before the test starts.
    }

    await login(page);
    await goToIdentityProviders(page);
  });

  test.afterEach(async () => {
    try {
      await adminClient.deleteIdentityProvider(alias);
    } catch {
      // The provider may already have been deleted.
    }
  });

  test("should expose and persist pass login_hint for Google", async ({
    page,
  }) => {
    await page.getByTestId("google-card").click();
    await expect(page.locator("#passLoginHint")).toBeVisible();

    await page.getByTestId("alias").fill(alias);
    await page.getByTestId("config.clientId").fill("client");
    await page.getByTestId("config.clientSecret").fill("secret");
    await switchOn(page, "#passLoginHint");
    await page.getByTestId("createProvider").click();

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );
    await expect(page.locator("#passLoginHint")).toBeChecked();

    await switchOff(page, "#passLoginHint");
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");
    await expect(page.locator("#passLoginHint")).not.toBeChecked();

    await switchOn(page, "#passLoginHint");
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");
    await expect(page.locator("#passLoginHint")).toBeChecked();
  });
});
