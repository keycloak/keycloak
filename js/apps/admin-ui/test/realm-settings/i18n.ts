import { expect, type Page, test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";

export const THEME_LOCALIZATION_UNAVAILABLE_MESSAGE =
  "Theme localization tests require Keycloak with the keycloak.v3 admin theme and community translations.";

export async function skipIfThemeLocalizationUnavailable() {
  const isAvailable = await adminClient.isThemeLocalizationAvailable();

  // eslint-disable-next-line playwright/no-skipped-test -- Skipped when the server lacks keycloak.v3 community messages.
  test.skip(!isAvailable, THEME_LOCALIZATION_UNAVAILABLE_MESSAGE);
}

export async function assertRealmSettingsText(
  page: Page,
  expectedText: string,
) {
  const element = page.locator("#nav-item-realm-settings");
  await expect(element).toContainText(expectedText);
}

export async function assertProviderCardText(
  page: Page,
  provider: string,
  expectedText: string,
) {
  const card = page.locator(`[data-testid="${provider}-card"]`);
  await expect(card).toContainText(expectedText);
}
