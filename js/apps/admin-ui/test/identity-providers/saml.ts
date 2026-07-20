import { type Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import {
  assertInvalidUrlNotification,
  clickSaveButton,
  setUrl,
} from "./main.ts";

export async function editSAMLSettings(page: Page, samlProviderName: string) {
  const providerEnabledSwitch = page.locator("#-switch");
  // Toggle provider state
  if (await providerEnabledSwitch.isChecked()) {
    await providerEnabledSwitch.click({ force: true });
    await confirmModal(page);
    await assertNotificationMessage(page, "Provider successfully updated");
  }
  await goToIdentityProviders(page);
  await expect(page.getByText("Disabled")).toBeVisible();

  await clickTableRowItem(page, samlProviderName);
  if (!(await providerEnabledSwitch.isChecked())) {
    await providerEnabledSwitch.click({ force: true });
  }

  // Verify and configure settings
  await setUrl(page, "singleSignOnService", "invalid");
  await clickSaveButton(page);
  await assertInvalidUrlNotification(page, "singleSignOnService");
  await setUrl(page, "singleSignOnService", "https://valid.com");

  await setUrl(page, "singleLogoutService", "invalid");
  await clickSaveButton(page);
  await assertInvalidUrlNotification(page, "singleLogoutService");
  await setUrl(page, "singleLogoutService", "https://valid.com");

  await selectItem(
    page,
    page.locator("#config\\.nameIDPolicyFormat"),
    "Kerberos",
  );
  await selectItem(
    page,
    page.locator("#config\\.principalType"),
    "Attribute [Name]",
  );

  // Toggle SAML switches
  const switches = [
    page.getByTestId("config.allowCreate"),
    page.getByTestId("config.wantAssertionsEncrypted"),
    page.getByTestId("config.forceAuthn"),
  ];
  for (const field of switches) {
    await field.check({ force: true });
  }

  await page.getByTestId("config.sendIdTokenOnLogout").uncheck({ force: true });

  // Ensure there is always a persisted change even when defaults already match.
  await page
    .getByTestId("displayName")
    .fill(`SAML edited ${Date.now().toString().slice(-6)}`);

  await clickSaveButton(page);
}
