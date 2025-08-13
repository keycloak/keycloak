import { Locator, Page, expect } from "@playwright/test";

export async function searchItem(
  page: Page,
  placeHolder: string,
  itemName: string,
) {
  await page.locator("table tbody").waitFor();
  await page.getByPlaceholder(placeHolder).fill(itemName);
  await page.keyboard.press("Enter");
}

export async function clearAllFilters(page: Page) {
  await page.getByTestId("clear-all-filters-empty-action").click();
}

export async function clickTableRowItem(page: Page, itemName: string) {
  await page.getByRole("link", { name: itemName }).first().click();
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

export async function assertRowExists(
  page: Page,
  itemName: string,
  exist = true,
) {
  if (exist) {
    await expect(page.getByRole("row", { name: itemName })).toBeVisible();
  } else {
    await expect(page.getByRole("row", { name: itemName })).toBeHidden();
  }
}

export async function assertNoResults(page: Page) {
  await expect(
    page.getByRole("heading", { name: "No search results" }),
  ).toBeVisible();
}

export async function clickTableToolbarItem(
  page: Page,
  itemName: string,
  kebab = false,
) {
  if (kebab) {
    await page.getByTestId("kebab").click();
  }
  return page
    .locator(`[data-testid="table-toolbar"]`)
    .getByText(itemName)
    .click();
}

export async function getTableData(page: Page, name: string) {
  const rowsLocator = await getTableRows(page, name);
  const rows = await rowsLocator.elementHandles();
  const tableData = await Promise.all(
    rows.map(async (row) => {
      const cells = await row.$$("td");
      return await Promise.all(cells.map((cell) => cell.innerText()));
    }),
  );
  return tableData;
}

export async function assertTableRowsLength(
  page: Page,
  name: string,
  length: number,
): Promise<void> {
  const rows = await getTableRows(page, name);
  await expect(rows).toHaveCount(length);
}

async function getTableRows(page: Page, name: string): Promise<Locator> {
  const table = page
    .getByRole("grid")
    .and(page.getByLabel(name, { exact: true }));
  await table.locator("tbody").waitFor();
  return table.locator("tbody tr");
}

export async function clickNextPageButton(page: Page) {
  await page
    .getByLabel("Pagination bottom")
    .getByLabel("Go to next page")
    .click();
}

export async function assertEmptyTable(page: Page) {
  await expect(page.getByTestId("empty-state")).toBeVisible();
}

export async function clickSelectRow(
  page: Page,
  tableName: string,
  row: number | string,
) {
  if (typeof row === "string") {
    const rows = await getTableData(page, tableName);
    const rowIndex = rows.findIndex((r) => r.includes(row as string));
    if (rowIndex === -1) {
      throw new Error(`Row ${row} not found: ${rows}`);
    }
    row = rowIndex;
  }
  await page.getByLabel(tableName).getByLabel(`Select row ${row}`).click();
}

export async function expandRow(page: Page, tableName: string, row: number) {
  await page
    .getByLabel(tableName)
    .locator(`button[id="expandable-row-${row}"]`)
    .click();
}

export async function refreshTable(page: Page) {
  await page.getByTestId("refresh").click();
}
