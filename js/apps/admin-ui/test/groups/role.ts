import { Page } from "@playwright/test";

export async function goToRoleMappingTab(page: Page) {
  await page.getByTestId("role-mapping-tab").click();
}

export async function assignRole(page: Page) {
  await page.getByTestId("no-roles-for-this-group-empty-action").click();
}
