import { type Page, expect } from "@playwright/test";
import {
  clickSwitch,
  ensureSwitchOff,
  selectItem,
  switchOn,
} from "../utils/form.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import {
  assertInvalidUrlNotification,
  clickSaveButton,
  setUrl,
} from "./main.ts";

const EDITED_DISPLAY_NAME = "SAML edited for save";

export async function editSAMLSettings(page: Page, samlProviderName: string) {
  const providerEnabledSwitch = page.getByTestId(`${samlProviderName}-switch`);
  await expect(providerEnabledSwitch).toBeChecked();
  await clickSwitch(page, providerEnabledSwitch);
  await expect(page.getByTestId("confirm")).toBeVisible();
  await confirmModal(page);
  await assertNotificationMessage(page, "Provider successfully updated");
  await goToIdentityProviders(page);
  await expect(page.getByText("Disabled")).toBeVisible();

  await clickTableRowItem(page, samlProviderName);
  await expect(providerEnabledSwitch).not.toBeChecked();
  await switchOn(page, providerEnabledSwitch);

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
    await switchOn(page, field);
  }

  await ensureSwitchOff(page, page.getByTestId("config.sendIdTokenOnLogout"));

  // Keep this deterministic while ensuring the form is dirty before saving.
  await page.getByTestId("displayName").fill(EDITED_DISPLAY_NAME);

  await clickSaveButton(page);
}
