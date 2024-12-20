import { expect, Page } from "@playwright/test";

export async function assertNotificationMessage(page: Page, message: string) {
  await expect(page.getByTestId("last-alert")).toHaveText(message);
}

function getActionToggleButton(page: Page) {
  return page.getByTestId("action-dropdown");
}

export async function selectActionToggleItem(page: Page, item: string) {
  await getActionToggleButton(page).click();
  await page.getByRole("menuitem", { name: item, exact: true }).click();
}
