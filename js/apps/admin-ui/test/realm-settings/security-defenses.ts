import type { Page } from "@playwright/test";

export async function goToSecurityDefensesTab(page: Page) {
  await page.getByTestId("rs-security-defenses-tab").click();
}

export async function clickSaveSecurityDefenses(page: Page) {
  await page.getByTestId("headers-form-tab-save").click();
}

export async function fillWaitIncrementSeconds(page: Page, value: string) {
  await page.getByTestId("waitIncrementSeconds").fill(value);
}

export async function fillMaxFailureWaitSeconds(page: Page, value: string) {
  await page.getByTestId("maxFailureWaitSeconds").fill(value);
}

export async function fillMaxDeltaTimeSeconds(page: Page, value: string) {
  await page.getByTestId("maxDeltaTimeSeconds").fill(value);
}

export async function fillMinimumQuickLoginWaitSeconds(
  page: Page,
  value: string,
) {
  await page.getByTestId("minimumQuickLoginWaitSeconds").fill(value);
}

export async function clickSaveBruteForce(page: Page) {
  await page.getByTestId("brute-force-tab-save").click();
}
