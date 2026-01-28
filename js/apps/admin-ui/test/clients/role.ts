import { expect, type Page } from "@playwright/test";
import {
  pickRoleType,
  confirmModalAssign,
  pickRole,
  type RoleType,
} from "../utils/roles.ts";

export async function goToRolesTab(page: Page) {
  await page.getByTestId("rolesTab").click();
}

export async function goToCreateRoleFromEmptyState(page: Page) {
  await page.getByTestId("no-roles-for-this-client-empty-action").click();
}

export async function goToCreateRole(page: Page) {
  await page.getByTestId("create-role").click();
}

function getDescription(page: Page) {
  return page.getByTestId("description");
}

export async function fillRoleData(page: Page, name: string, description = "") {
  const nameInput = page.getByTestId("name");
  await nameInput.waitFor();
  const isDisabled = await nameInput.isDisabled();
  if (!isDisabled) {
    await nameInput.fill(name);
  }
  await getDescription(page).fill(description);
}

export async function assertDescriptionValue(page: Page, value: string) {
  await expect(getDescription(page)).toHaveValue(value);
}

export async function goToAssociatedRolesTab(page: Page) {
  await page.getByTestId("associatedRolesTab").click();
}

export async function addAssociatedRoles(
  page: Page,
  roleName: string,
  roleType: RoleType = "roles",
) {
  await pickRoleType(page, roleType);
  await pickRole(page, roleName, true);
  await confirmModalAssign(page);
}
