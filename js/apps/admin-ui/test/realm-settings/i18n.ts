import { expect, type Page } from "@playwright/test";

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
