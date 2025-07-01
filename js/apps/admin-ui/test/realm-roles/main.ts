import { Page, expect } from "@playwright/test";

export async function clickCreateRoleButton(page: Page) {
  await page.locator("text=Create role").click();
}

export async function goToAssociatedRolesTab(page: Page) {
  await page.getByTestId("associatedRolesTab").click();
}

export async function assertUnassignDisabled(page: Page) {
  await expect(page.getByTestId("unAssignRole")).toBeDisabled();
}
