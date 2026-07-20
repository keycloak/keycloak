import { type Locator, type Page, expect } from "@playwright/test";

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
  const rows = page.locator("table tbody tr");
  const exactRow = rows
    .filter({ has: page.getByText(itemName, { exact: true }) })
    .first();

  if ((await exactRow.count()) > 0) {
    const exactRowLink = exactRow.getByRole("link", {
      name: itemName,
      exact: true,
    });
    if ((await exactRowLink.count()) > 0) {
      await exactRowLink.first().click();
      return;
    }

    const firstExactRowLink = exactRow.getByRole("link").first();
    if ((await firstExactRowLink.count()) > 0) {
      await firstExactRowLink.click();
      return;
    }
  }

  const partialRow = rows.filter({ hasText: itemName }).first();

  if ((await partialRow.count()) > 0) {
    const exactRowLink = partialRow.getByRole("link", {
      name: itemName,
      exact: true,
    });
    if ((await exactRowLink.count()) > 0) {
      await exactRowLink.first().click();
      return;
    }

    const partialRowLink = partialRow.getByRole("link", { name: itemName });
    if ((await partialRowLink.count()) > 0) {
      await partialRowLink.first().click();
      return;
    }

    const firstRowLink = partialRow.getByRole("link").first();
    if ((await firstRowLink.count()) > 0) {
      await firstRowLink.click();
      return;
    }
  }

  const exactLink = page.getByRole("link", { name: itemName, exact: true });
  if ((await exactLink.count()) > 0) {
    await exactLink.first().click();
    return;
  }

  await page.getByRole("link", { name: itemName }).first().click();
}

export function getRowByCellText(page: Page, cellText: string): Locator {
  return page
    .locator("table tbody tr")
    .filter({ has: page.getByText(cellText, { exact: true }) });
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
  const row = page.locator("table tbody").getByRole("row", { name: itemName });
  if (exist) {
    await expect(row.first()).toBeVisible();
  } else {
    await expect(row).toHaveCount(0);
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
  const toolbar = page.getByTestId("table-toolbar");
  if (kebab) {
    await toolbar.getByTestId("kebab").click();
    const exactMenuItem = page.getByRole("menuitem", {
      name: itemName,
      exact: true,
    });
    if ((await exactMenuItem.count()) > 0) {
      await exactMenuItem.first().click();
      return;
    }
    await page.getByRole("menuitem", { name: itemName }).first().click();
    return;
  }

  const exactToolbarItem = toolbar
    .getByRole("button", { name: itemName, exact: true })
    .or(toolbar.getByRole("link", { name: itemName, exact: true }))
    .first();
  try {
    await exactToolbarItem.waitFor({ state: "visible", timeout: 2_000 });
    await exactToolbarItem.click();
    return;
  } catch {
    // Fall through to partial name and overflow menu attempts.
  }

  const partialToolbarItem = toolbar
    .getByRole("button", { name: itemName })
    .or(toolbar.getByRole("link", { name: itemName }))
    .first();
  try {
    await partialToolbarItem.waitFor({ state: "visible", timeout: 2_000 });
    await partialToolbarItem.click();
    return;
  } catch {
    // Fall through to overflow menu attempt.
  }

  const overflowKebab = toolbar.getByTestId("kebab");
  if ((await overflowKebab.count()) > 0) {
    await overflowKebab.click();
    const exactMenuItem = page.getByRole("menuitem", {
      name: itemName,
      exact: true,
    });
    if ((await exactMenuItem.count()) > 0) {
      await exactMenuItem.first().click();
      return;
    }
    await page.getByRole("menuitem", { name: itemName }).first().click();
    return;
  }

  throw new Error(`Toolbar item "${itemName}" not found`);
}

export async function getTableData(page: Page, name: string) {
  const rowsLocator = await getTableRows(page, name);
  const rowCount = await rowsLocator.count();
  const tableData: string[][] = [];

  for (let rowIndex = 0; rowIndex < rowCount; rowIndex++) {
    const row = rowsLocator.nth(rowIndex);
    const cells = row.locator("td");
    const cellCount = await cells.count();
    const rowData: string[] = [];

    for (let cellIndex = 0; cellIndex < cellCount; cellIndex++) {
      rowData.push((await cells.nth(cellIndex).innerText()).trim());
    }

    tableData.push(rowData);
  }

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

export async function openRowDetails(page: any, itemName: string) {
  const row = page.getByRole("row", { name: itemName });
  await row.getByRole("button", { name: "Details" }).click();
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
