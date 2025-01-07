import { Locator, Page, expect } from "@playwright/test";

export async function searchItem(
  page: Page,
  placeHolder: string,
  itemName: string,
) {
  await page.getByPlaceholder(placeHolder).fill(itemName);
  await page.keyboard.press("Enter");
}

export function clickTableRowItem(page: Page, itemName: string) {
  return page.getByRole("link", { name: itemName }).click();
}

export function getRowByCellText(page: Page, cellText: string): Locator {
  return page.getByText(cellText, { exact: true });
}

export function selectRowAction(page: Page, action: string) {
  return page.getByRole("menuitem", { name: action }).click();
}

export function selectRowKebab(page: Page, itemName: string) {
  return page
    .getByRole("row", { name: itemName })
    .getByLabel("Kebab toggle")
    .click();
}

export function clickRowKebabItem(page: Page, itemName: string) {
  return page.getByRole("menuitem", { name: itemName }).click();
}

export async function assertRowExists(page: Page, itemName: string) {
  await expect(page.getByRole("row", { name: itemName })).toBeVisible();
}
