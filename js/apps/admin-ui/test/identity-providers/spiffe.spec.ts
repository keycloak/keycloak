import { test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { clickSaveButton, createSPIFFEProvider } from "./main.ts";

test.beforeEach(async ({ page }) => {
  await login(page);
  await goToIdentityProviders(page);
});

test.afterAll(() => adminClient.deleteIdentityProvider("spiffe"));

test.describe.serial("SPIFFE identity provider test", () => {
  test("should create a SPIFFE provider", async ({ page }) => {
    await createSPIFFEProvider(
      page,
      "spiffe",
      "spiffe://mytrust2",
      "https://mytrust",
    );

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );

    await goToIdentityProviders(page);
    await clickTableRowItem(page, "Spiffe");

    await page.getByTestId("config.trustDomain").fill("spiffe://mytrust2");
    await page.getByTestId("config.bundleEndpoint").fill("https://mytrust2");

    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");
  });
});
