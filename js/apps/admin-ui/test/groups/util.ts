import { Page } from "@playwright/test";
import { clickTableRowItem } from "../utils/table";

export async function goToGroupDetails(page: Page, name: string) {
  await clickTableRowItem(page, name);
}

export async function goToChildGroupsTab(page: Page) {
  await page.getByTestId("groups").click();
}
