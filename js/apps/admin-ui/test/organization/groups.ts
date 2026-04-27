import type { Page } from "@playwright/test";

// When inside an org group detail, two `action-dropdown` buttons are present:
// one from the org header ("Action") and one from the group header ("Group action").
// This helper targets the group-level dropdown specifically.
export async function selectOrgGroupActionToggleItem(page: Page, item: string) {
  await page.getByRole("button", { name: "Group action" }).click();
  await page.getByRole("menuitem", { name: item, exact: true }).click();
}

export async function goToOrgGroupsTab(page: Page) {
  await page.getByTestId("groupsTab").click();
}

export async function createOrgGroup(
  page: Page,
  name: string,
  description: string,
  fromEmptyState = false,
) {
  if (fromEmptyState) {
    // testid is derived from the translated message text (which has a typo: "orgainization")
    await page
      .getByTestId("no-groups-in-this-orgainization-empty-action")
      .click();
  } else {
    await page.getByTestId("openCreateGroupModal").click();
  }
  await page.getByTestId("name").fill(name);
  await page.getByTestId("description").fill(description);
  await page.getByTestId("createGroup").click();
}
