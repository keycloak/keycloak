import { Page } from "@playwright/test";
import { clickSelectRow } from "./table";

export type RoleType = "client" | "roles";
const rolePickTableName = "Roles";

export async function changeRoleTypeFilter(page: Page, roleType: RoleType) {
  const currentFilter = await page
    .getByTestId("filter-type-dropdown")
    .innerText();
  if (currentFilter.includes(roleType)) {
    return;
  }

  let filter;
  if (roleType === "client") {
    filter = "Filter by clients";
  } else {
    filter = "Filter by realm roles";
  }

  await page.getByTestId("filter-type-dropdown").click();
  await page.getByRole("menuitem", { name: filter, exact: true }).click();
}

export async function pickRole(page: Page, roleName: string) {
  await clickSelectRow(page, rolePickTableName, roleName);
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
