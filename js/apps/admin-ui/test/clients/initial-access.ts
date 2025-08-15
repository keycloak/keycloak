import { type Page, expect } from "@playwright/test";
import { assertFieldError } from "../utils/form.ts";

export async function goToInitialAccessTokenTab(page: Page) {
  await page.getByTestId("initialAccessToken").click();
}

export async function assertInitialAccessTokensIsEmpty(page: Page) {
  await expect(
    page.getByTestId("no-initial-access-tokens-empty-action"),
  ).toBeVisible();
}

export async function assertInitialAccessTokensIsNotEmpty(page: Page) {
  await expect(
    page.getByTestId("no-initial-access-tokens-empty-action"),
  ).toBeHidden();
}

export async function goToCreateFromEmptyList(page: Page) {
  await page.getByTestId("no-initial-access-tokens-empty-action").click();
}

export async function fillNewTokenData(
  page: Page,
  days: number,
  count: number,
) {
  await page.getByTestId("expiration").fill(days.toString());

  for (let i = 0; i < count; i++) {
    await page.locator('#count [aria-label="Plus"]').click();
  }
}

export async function assertExpirationGreaterThanZeroError(page: Page) {
  await assertFieldError(
    page,
    "expiration",
    "Value should be greater than or equal to 1",
  );
}

export async function assertCountValue(page: Page, expectedCount: number) {
  await expect(page.locator("#count input")).toHaveValue(
    expectedCount.toString(),
  );
}

export async function checkSaveButtonIsDisabled(page: Page) {
  await expect(page.getByTestId("save")).toBeDisabled();
}

export async function assertClipboardContent(page: Page) {
  await page.locator('[data-testid="initialAccessToken"] button').click();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toMatch(
    /^[0-9A-Za-z.\-_]{300,}$/,
  );
}

export async function closeModal(page: Page) {
  await page.getByLabel("Close", { exact: true }).click();
}
