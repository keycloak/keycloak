import { expect, type Page } from "@playwright/test";

export async function goToLoginTab(page: Page) {
  await page.getByTestId("rs-login-tab").click();
}

export async function toggleLoginSettingAndExpectSuccess(
  page: Page,
  switchTestId: string,
) {
  await page.locator(`[data-testid="${switchTestId}"]`).click({ force: true });
  await expect(page.getByTestId("last-alert")).toContainText(
    /changed successfully/i,
  );
}
