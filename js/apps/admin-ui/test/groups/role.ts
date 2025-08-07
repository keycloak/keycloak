import type { Page } from "@playwright/test";

export async function goToRoleMappingTab(page: Page) {
  await page.getByTestId("role-mapping-tab").click();
}
