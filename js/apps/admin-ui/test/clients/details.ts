import { expect, type Page } from "@playwright/test";
import { selectItem, assertSelectValue, switchToggle } from "../utils/form.ts";

function getKeyForCodeExchangeInput(page: Page) {
  return page.locator("#keyForCodeExchange");
}

export async function selectKeyForCodeExchangeInput(page: Page, value: string) {
  await selectItem(page, getKeyForCodeExchangeInput(page), value);
}

export async function assertKeyForCodeExchangeInput(page: Page, value: string) {
  await assertSelectValue(getKeyForCodeExchangeInput(page), value);
}

export async function toggleFrontchannelLogout(page: Page) {
  await switchToggle(page, "#kc-frontchannelLogout-switch");
}

export async function goToDedicatedScope(page: Page, clientId: string) {
  await page.getByTestId("clientScopesTab").click();
  await page.getByRole("link", { name: `${clientId}-dedicated` }).click();
}

export async function assertDedicatedScopeMapper(
  page: Page,
  mapperName: string,
) {
  await page.getByTestId("mappersTab").click();
  await expect(page.getByRole("link", { name: mapperName })).toBeVisible();
}

export async function toggleFullScopeAllowed(page: Page) {
  await page.getByTestId("scopeTab").click();
  await switchToggle(page, "#fullScopeAllowed");
}
