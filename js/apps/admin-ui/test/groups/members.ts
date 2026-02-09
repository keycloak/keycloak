import type { Page } from "@playwright/test";
import { clickSelectRow, clickTableToolbarItem } from "../utils/table.ts";

export async function goToMembersTab(page: Page) {
  await page.getByTestId("members").click();
}

export async function addMember(
  page: Page,
  usernames: string[],
  fromEmptyState = false,
) {
  if (fromEmptyState) {
    await page.getByTestId("no-users-found-empty-action").click();
  } else {
    await page.getByTestId("addMember").click();
  }

  for (const username of usernames) {
    await clickSelectRow(page, "Users", username);
  }

  await page.getByTestId("add").click();
}

export async function toggleIncludeSubGroupUsers(page: Page) {
  await page.getByTestId("includeSubGroupsCheck").click();
}

export async function leaveGroup(page: Page) {
  await clickTableToolbarItem(page, "Leave", true);
}
