import { expect, type Page } from "@playwright/test";

export async function goToLoginTab(page: Page) {
  await page.getByTestId("rs-login-tab").click();
}

export async function toggleLoginSettingAndExpectSuccess(
  page: Page,
  switchTestId: string,
) {
  const previousAlert = page.getByTestId("last-alert");
  if (await previousAlert.isVisible()) {
    await previousAlert.locator("button").click();
    await expect(previousAlert).toBeHidden();
  }

  await page.getByTestId(switchTestId).click();
  await expect(page.getByTestId("last-alert")).toContainText(
    /changed successfully/i,
  );
}
