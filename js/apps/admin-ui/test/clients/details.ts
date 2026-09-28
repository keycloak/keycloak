import type { Page } from "@playwright/test";
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

const jwtAuthorizationGrantIdpSelect =
  "#attributes\\.oauth2🍺jwt🍺authorization🍺grant🍺idp";

export async function enableJwtAuthorizationGrant(page: Page) {
  await page.getByTestId("jwt-authorization-grant-enabled").check();
}

export async function getJwtAuthorizationGrantIdpOptions(
  page: Page,
): Promise<string[]> {
  await page.keyboard.press("Escape");
  await page.locator(jwtAuthorizationGrantIdpSelect).click();
  const options = page.getByRole("option");
  await options.first().waitFor({ state: "visible" });
  return await options.allTextContents();
}
