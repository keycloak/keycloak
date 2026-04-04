import type { Page } from "@playwright/test";

export async function goToScopeTab(page: Page) {
  await page.getByTestId("scopeTab").click();
}
