import { type Page, expect } from "@playwright/test";
import { clickTableRowItem } from "../utils/table.ts";
import { selectItem } from "../utils/form.ts";

export async function goToKeys(page: Page) {
  await page.getByTestId("rs-keys-tab").click();
}

export async function goToAddProviders(page: Page) {
  await page.getByTestId("rs-providers-tab").click();
}

export async function goToDetails(page: Page, providerName: string) {
  await clickTableRowItem(page, providerName);
}

export async function assertPriority(page: Page, priority: string) {
  await expect(page.getByTestId("priority")).toHaveValue(priority);
}

export async function switchToFilter(page: Page, filter = "Active keys") {
  await selectItem(page, page.getByTestId("keysListinput"), filter);
}
