import { Page } from "@playwright/test";

export async function goToScopeTab(page: Page) {
  await page.getByTestId("scopeTab").click();
}

export async function assignRole(page: Page) {
  await page.getByTestId("no-roles-for-this-client-scope-empty-action").click();
}

export async function changeRoleTypeFilter(page: Page, roleType: string) {
  let filter;
  if (roleType === "client") {
    filter = "Filter by clients";
  } else {
    filter = "Filter by realm roles";
  }

  await page.getByTestId("filter-type-dropdown").click();
  await page.getByRole("menuitem", { name: filter, exact: true }).click();
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
