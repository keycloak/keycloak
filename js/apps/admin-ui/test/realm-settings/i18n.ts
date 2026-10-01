import { expect, type Page, test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";

export const ADMIN_V3_THEME_UNAVAILABLE_MESSAGE =
  "Theme localization tests require Keycloak with the keycloak.v3 admin theme.";

export const GERMAN_THEME_LOCALIZATION_UNAVAILABLE_MESSAGE =
  "German theme localization tests require the keycloak.v3 admin theme with community translations.";

export async function skipIfAdminV3ThemeUnavailable() {
  const isAvailable = await adminClient.isAdminV3ThemeAvailable();

  // eslint-disable-next-line playwright/no-skipped-test -- Skipped when the server lacks the keycloak.v3 admin theme.
  test.skip(!isAvailable, ADMIN_V3_THEME_UNAVAILABLE_MESSAGE);
}

export async function skipIfGermanThemeLocalizationUnavailable() {
  const isAvailable = await adminClient.isGermanThemeLocalizationAvailable();

  // eslint-disable-next-line playwright/no-skipped-test -- Skipped when the server lacks keycloak.v3 German community messages.
  test.skip(!isAvailable, GERMAN_THEME_LOCALIZATION_UNAVAILABLE_MESSAGE);
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
