import { expect, Page } from "@playwright/test";
import {
  changeRoleTypeFilter,
  confirmModalAssign,
  pickRole,
  RoleType,
} from "../utils/roles";

export async function goToRolesTab(page: Page) {
  await page.getByTestId("rolesTab").click();
}

export async function goToCreateRoleFromEmptyState(page: Page) {
  await page.getByTestId("no-roles-for-this-client-empty-action").click();
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

export async function goToAttributesTab(page: Page) {
  await page.getByTestId("attributesTab").click();
}

export async function fillAttributeData(
  page: Page,
  key: string,
  value: string,
) {
  await page.getByTestId("attributes-add-row").click();
  await page.getByTestId("attributes-key").fill(key);
  await page.getByTestId("attributes-value").fill(value);
}

export async function deleteAttribute(page: Page, row: number) {
  await page.getByTestId("attributes-remove").nth(row).click();
}

export async function clickAttributeSaveButton(page: Page) {
  await page.getByTestId("attributes-save").click();
}

export async function assertAttributeLength(page: Page, length: number) {
  const rows = await page.getByTestId("attributes-key").all();
  expect(rows.length).toBe(length);
}

export async function goToAssociatedRolesTab(page: Page) {
  await page.getByTestId("associatedRolesTab").click();
}

export async function addAssociatedRoles(
  page: Page,
  roleName: string,
  roleType: RoleType = "roles",
) {
  await changeRoleTypeFilter(page, roleType);
  await pickRole(page, roleName);
  await confirmModalAssign(page);
}
