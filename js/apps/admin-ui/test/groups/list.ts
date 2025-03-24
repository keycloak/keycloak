import { Page } from "@playwright/test";

export async function createGroup(
  page: Page,
  name: string,
  fromEmptyState = false,
) {
  if (fromEmptyState) {
    await page.getByTestId("no-groups-in-this-realm-empty-action").click();
  } else {
    await page.getByTestId("openCreateGroupModal").click();
  }
  await page.getByTestId("name").fill(name);
  await page.getByTestId("createGroup").click();
}

export async function searchGroup(page: Page, name: string) {
  await page.getByTestId("group-search").locator("input").fill(name);
  await page
    .getByTestId("group-search")
    .getByLabel("Search", { exact: true })
    .click();
}

export async function renameGroup(page: Page, name: string) {
  await page.getByTestId("name").fill(name);
  await page.getByTestId("renameGroup").click();
}
