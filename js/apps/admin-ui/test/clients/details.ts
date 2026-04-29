import type { Page, Locator } from "@playwright/test";
import { expect } from "@playwright/test";
import {
  selectItem,
  assertSelectValue,
  switchToggle,
  switchOn,
} from "../utils/form.ts";

const REALM_ACTIVE_KEY_TEXT = "Use default (realm's active signing key)";

// SigningKeySelect displays keys as "algorithm - kid", so we match by kid suffix
// Pass empty string to select realm default key
async function selectSigningKeyByKid(
  page: Page,
  field: Locator,
  keyId: string,
) {
  await field.click();
  if (keyId === "") {
    await page
      .getByRole("option")
      .filter({ hasText: REALM_ACTIVE_KEY_TEXT })
      .click();
  } else {
    // Find option that contains the keyId (format: "algorithm - kid")
    await page
      .getByRole("option")
      .filter({ hasText: new RegExp(`- ${keyId}$`) })
      .click();
  }
}

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

function getAccessTokenSigningKeyIdSelect(page: Page) {
  return page.locator("#attributes\\.access🍺token🍺signed🍺response🍺kid");
}

function getIdTokenSigningKeyIdSelect(page: Page) {
  return page.locator("#attributes\\.id🍺token🍺signed🍺response🍺kid");
}

function getUserInfoSigningKeyIdSelect(page: Page) {
  return page.locator("#attributes\\.user🍺info🍺response🍺signature🍺kid");
}

function getAuthorizationResponseSigningKeyIdSelect(page: Page) {
  return page.locator("#attributes\\.authorization🍺signed🍺response🍺kid");
}

export async function assertAllSigningKeyDropdownsVisible(page: Page) {
  await expect(getAccessTokenSigningKeyIdSelect(page)).toBeVisible();
  await expect(getIdTokenSigningKeyIdSelect(page)).toBeVisible();
  await expect(getUserInfoSigningKeyIdSelect(page)).toBeVisible();
  await expect(getAuthorizationResponseSigningKeyIdSelect(page)).toBeVisible();
}

export async function selectAccessTokenSigningKey(page: Page, keyId: string) {
  await selectSigningKeyByKid(
    page,
    getAccessTokenSigningKeyIdSelect(page),
    keyId,
  );
}

export async function selectIdTokenSigningKey(page: Page, keyId: string) {
  await selectSigningKeyByKid(page, getIdTokenSigningKeyIdSelect(page), keyId);
}

export async function selectUserInfoSigningKey(page: Page, keyId: string) {
  await selectSigningKeyByKid(page, getUserInfoSigningKeyIdSelect(page), keyId);
}

export async function selectAuthorizationResponseSigningKey(
  page: Page,
  keyId: string,
) {
  await selectSigningKeyByKid(
    page,
    getAuthorizationResponseSigningKeyIdSelect(page),
    keyId,
  );
}

// Assert signing key value - uses regex to match "algorithm - kid" format
async function assertSigningKeyValue(field: Locator, expectedKid: string) {
  if (expectedKid === "") {
    await expect(field).toHaveText(REALM_ACTIVE_KEY_TEXT);
  } else {
    await expect(field).toHaveText(new RegExp(`- ${expectedKid}$`));
  }
}

// Assert signing key displays with specific format text
export async function assertAccessTokenSigningKeyDisplayText(
  page: Page,
  expectedText: string | RegExp,
) {
  await expect(getAccessTokenSigningKeyIdSelect(page)).toHaveText(expectedText);
}

export async function assertAccessTokenSigningKeyValue(
  page: Page,
  expectedValue: string,
) {
  await assertSigningKeyValue(
    getAccessTokenSigningKeyIdSelect(page),
    expectedValue,
  );
}

export async function assertIdTokenSigningKeyValue(
  page: Page,
  expectedValue: string,
) {
  await assertSigningKeyValue(
    getIdTokenSigningKeyIdSelect(page),
    expectedValue,
  );
}

export async function assertUserInfoSigningKeyValue(
  page: Page,
  expectedValue: string,
) {
  await assertSigningKeyValue(
    getUserInfoSigningKeyIdSelect(page),
    expectedValue,
  );
}

export async function assertAuthorizationResponseSigningKeyValue(
  page: Page,
  expectedValue: string,
) {
  await assertSigningKeyValue(
    getAuthorizationResponseSigningKeyIdSelect(page),
    expectedValue,
  );
}
