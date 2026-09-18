import type { Page } from "@playwright/test";
import { expect } from "@playwright/test";
import {
  selectItem,
  assertSelectValue,
  switchToggle,
  switchOn,
} from "../utils/form.ts";

function getKeyForCodeExchangeInput(page: Page) {
  return page.locator("#keyForCodeExchange");
}

export async function selectKeyForCodeExchangeInput(page: Page, value: string) {
  await switchOn(page, page.getByTestId("pkce-required"));
  await selectItem(page, getKeyForCodeExchangeInput(page), value);
}

export async function assertKeyForCodeExchangeInput(page: Page, value: string) {
  await assertSelectValue(getKeyForCodeExchangeInput(page), value);
}

export async function toggleLogoutConfirmation(page: Page) {
  const logoutConfirmationSwitch =
    "#attributes\\.logout🍺confirmation🍺enabled";
  await switchToggle(page, logoutConfirmationSwitch);
}

export async function setResourceUrl(page: Page, value: string) {
  await page.getByTestId("attributes.resource_url").fill(value);
}

export async function assertResourceUrl(page: Page, value: string) {
  await expect(page.getByTestId("attributes.resource_url")).toHaveValue(value);
}
