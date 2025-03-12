import { Page } from "@playwright/test";

export async function goToScopeTab(page: Page) {
  await page.getByTestId("scopeTab").click();
}

export async function assignRole(page: Page) {
  await page.getByTestId("no-roles-for-this-client-scope-empty-action").click();
}
