import { type Page, expect } from "@playwright/test";
import {
  selectItem,
  assertSelectValue,
  switchToggle,
  switchOn,
} from "../utils/form.ts";

function getKeyForCodeExchangeInput(page: Page) {
  return page.locator("#keyForCodeExchange");
}

function getClientAuthenticatorInput(page: Page) {
  return page.locator("#clientAuthenticatorType");
}

function getJwtAlgorithmInput(page: Page) {
  return page.locator("#attributes\\.token🍺endpoint🍺auth🍺signing🍺alg");
}

export async function goToCredentialsTab(page: Page) {
  await page.getByRole("tab", { name: "Credentials" }).click();
}

export async function selectClientAuthenticator(page: Page, value: string) {
  await selectItem(page, getClientAuthenticatorInput(page), value);
}

export async function assertMacAlgorithmLabel(page: Page) {
  await expect(page.getByText("MAC algorithm", { exact: true })).toBeVisible();
  await expect(
    page.getByText("Signature algorithm", { exact: true }),
  ).toHaveCount(0);
}

export async function assertSignatureAlgorithmLabel(page: Page) {
  await expect(
    page.getByText("Signature algorithm", { exact: true }),
  ).toBeVisible();
  await expect(page.getByText("MAC algorithm", { exact: true })).toHaveCount(0);
}

export async function assertJwtAlgorithmOptions(
  page: Page,
  expected: string[],
  unexpected: string[] = [],
) {
  await getJwtAlgorithmInput(page).click();
  for (const algorithm of expected) {
    await expect(
      page.getByRole("option", { name: algorithm, exact: true }),
    ).toBeVisible();
  }
  for (const algorithm of unexpected) {
    await expect(
      page.getByRole("option", { name: algorithm, exact: true }),
    ).toHaveCount(0);
  }
  await page.keyboard.press("Escape");
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

export async function setResourceUrl(page: Page, value: string) {
  await page.getByTestId("attributes.resource_url").fill(value);
}

export async function assertResourceUrl(page: Page, value: string) {
  await expect(page.getByTestId("attributes.resource_url")).toHaveValue(value);
}
