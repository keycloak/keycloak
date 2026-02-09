import type { Page } from "@playwright/test";

export async function goToLoginTab(page: Page) {
  await page.getByTestId("rs-login-tab").click();
}
