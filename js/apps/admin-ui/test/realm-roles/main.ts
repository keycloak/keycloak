import { Page, expect } from "@playwright/test";

export async function clickCreateRoleButton(page: Page) {
  await page.locator("text=Create role").click();
}

export async function goToAssociatedRolesTab(page: Page) {
  await page.getByTestId("associatedRolesTab").click();
}

export async function clickAddRoleButton(page: Page, empty: boolean = false) {
  const id = empty ? "no-roles-in-this-realm-empty-action" : "assignRole";
  await page.getByTestId(id).click();
}

export async function assignRole(page: Page) {
  await page.getByTestId("no-roles-in-this-realm-empty-action").click();
}

export async function assertUnassignDisabled(page: Page) {
  await expect(page.getByTestId("unAssignRole")).toBeDisabled();
}
