import type { Page } from "@playwright/test";
import { clickSelectRow } from "./table.ts";

export type RoleType = "client" | "roles";
const rolePickTableName = "Role list";

export async function pickRoleType(page: Page, roleType: RoleType) {
  await page.getByTestId("add-role-mapping-button").click();

  let filter;
  if (roleType === "client") {
    filter = "Client roles";
  } else {
    filter = "Realm roles";
  }

  await page.getByRole("menuitem", { name: filter, exact: true }).click();
}

export async function pickRole(
  page: Page,
  roleName: string,
  dialog: boolean = false,
) {
  const name = dialog ? "Associated roles" : rolePickTableName;
  await clickSelectRow(page, name, roleName);
}

// client roles are paginated and sorted by client, so a role is not necessarily on the first page
export async function searchRoleInDialog(page: Page, search: string) {
  await page.getByRole("dialog").getByPlaceholder("Search").fill(search);
  await page.keyboard.press("Enter");
}

export async function confirmModalAssign(page: Page) {
  await page.getByTestId("assign").click();
}

export async function clickHideInheritedRoles(page: Page) {
  await page.getByTestId("hideInheritedRoles").click();
}

export async function clickUnassign(page: Page) {
  await page.getByTestId("unAssignRole").click();
}
