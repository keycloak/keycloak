import type { Page } from "@playwright/test";

export async function goToMembersTab(page: Page) {
  await page.getByTestId("membersTab").click();
}

export async function clickAddRealmUser(page: Page) {
  await page.getByTestId("add-realm-user-empty-action").click();
}
