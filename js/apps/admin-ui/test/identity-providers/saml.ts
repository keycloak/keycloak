import { type Page, expect } from "@playwright/test";
import { selectItem, switchOff, switchOn } from "../utils/form.ts";
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
  // Toggle provider state
  await switchOff(page, "#-switch");
  await confirmModal(page);
  await assertNotificationMessage(page, "Provider successfully updated");
  await goToIdentityProviders(page);
  await expect(page.getByText("Disabled")).toBeVisible();

  await clickTableRowItem(page, samlProviderName);
  await switchOn(page, "#-switch");

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
    "config.allowCreate",
    "config.wantAssertionsSigned",
    "config.wantAssertionsEncrypted",
    "config.forceAuthn",
  ];
  for (const switchId of switches) {
    await switchOn(page, `[data-testid="${switchId}"]`);
  }

  const switchOffIds = ["config.sendIdTokenOnLogout"];
  for (const switchId of switchOffIds) {
    await switchOff(page, `[data-testid="${switchId}"]`);
  }

  await clickSaveButton(page);
}
