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

export async function clickRowKebabItem(
  page: Page,
  itemName: string,
  action: string,
) {
  await page
    .getByRole("row", { name: itemName })
    .getByLabel("Kebab toggle")
    .click();
  await page.getByRole("menuitem", { name: action }).click();
}

export async function assertRowExists(page: Page, itemName: string) {
  await expect(page.getByRole("row", { name: itemName })).toBeVisible();
}

export async function assertNoResults(page: Page) {
  await expect(
    page.getByRole("heading", { name: "No search results" }),
  ).toBeVisible();
}

export async function clickTableToolbarItem(page: Page, itemName: string) {
  return page
    .locator(`[data-testid="table-toolbar"]`)
    .getByRole("link", { name: itemName })
    .click();
}

export async function getTableData(page: Page) {
  const tableData: string[][] = [];
  const rowCount = await page.locator("tbody tr").count();
  const columnCount = await page.locator("tbody tr:first-child td").count();

  for (let i = 0; i < rowCount; i++) {
    tableData.push([]);
    for (let j = 0; j < columnCount; j++) {
      tableData[i].push(
        await page
          .locator("tbody")
          .locator("tr")
          .nth(i)
          .locator("td")
          .nth(j)
          .innerText(),
      );
    }
  }
  return tableData;
}
