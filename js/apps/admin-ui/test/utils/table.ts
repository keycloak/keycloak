import { type Locator, type Page, expect } from "@playwright/test";
import { waitForLoadingCycle, waitForTableIdle } from "./loading.ts";

const TABLE_LOAD_TIMEOUT_MS = 15_000;

function escapeRegex(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function getTableRowLink(tableBody: Locator, itemName: string): Locator {
  const exactNameRegex = new RegExp(`^${escapeRegex(itemName)}$`, "i");

  return tableBody
    .getByRole("link", { name: exactNameRegex })
    .or(tableBody.getByRole("link", { name: itemName }))
    .or(
      tableBody.getByTestId("provider-name-link").filter({ hasText: itemName }),
    )
    .or(
      tableBody
        .locator("tr")
        .filter({ hasText: exactNameRegex })
        .getByRole("link")
        .first(),
    );
}

export async function searchItem(
  page: Page,
  placeHolder: string,
  itemName: string,
) {
  await page
    .locator("table tbody")
    .waitFor({ state: "visible", timeout: TABLE_LOAD_TIMEOUT_MS });
  await page.getByPlaceholder(placeHolder).fill(itemName);
  await page.keyboard.press("Enter");
  await waitForLoadingCycle(page);
}

export async function clearAllFilters(page: Page) {
  await page.getByTestId("clear-all-filters-empty-action").click();
}

export async function clickTableRowItem(page: Page, itemName: string) {
  const tableBody = page.locator("table tbody");
  const rowLink = getTableRowLink(tableBody, itemName);

  await expect(rowLink.first()).toBeVisible({ timeout: TABLE_LOAD_TIMEOUT_MS });
  await rowLink.first().click();
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
  const kebab = page
    .getByRole("row", { name: itemName })
    .getByLabel("Kebab toggle");
  const menuItem = page.getByRole("menuitem", { name: action });

  // Retry: the kebab dropdown can fail to stay open due to rendering races under CI load.
  await expect(async () => {
    await kebab.click();
    await menuItem.click({ timeout: 2_000 });
  }).toPass({ timeout: 10_000 });
}

export async function assertRowExists(
  page: Page,
  itemName: string,
  exist = true,
) {
  await waitForTableIdle(page);

  const row = page.locator("table tbody").getByRole("row", { name: itemName });
  if (exist) {
    await expect(row.first()).toBeVisible({ timeout: TABLE_LOAD_TIMEOUT_MS });
  } else {
    await expect(row).toHaveCount(0);
  }
}

export async function assertNoResults(page: Page) {
  await expect(
    page.getByRole("heading", { name: "No search results" }),
  ).toBeVisible();
}

async function resolveTableToolbar(page: Page): Promise<Locator> {
  const activeTabPanel = page.getByRole("tabpanel").filter({ visible: true });
  const tabPanelToolbar = activeTabPanel.getByTestId("table-toolbar");
  if ((await tabPanelToolbar.count()) > 0) {
    return tabPanelToolbar.first();
  }

  const toolbars = page.getByTestId("table-toolbar").filter({ visible: true });
  const toolbarCount = await toolbars.count();
  for (let index = 0; index < toolbarCount; index++) {
    const toolbar = toolbars.nth(index);
    const followingGrid = toolbar.locator(
      "xpath=following::*[@role='grid' or @role='treegrid'][1]",
    );
    const selectedRow = followingGrid.locator(
      'tbody tr input[type="checkbox"]:checked',
    );
    if ((await selectedRow.count()) > 0) {
      return toolbar;
    }
  }

  return toolbars.first();
}

export async function clickTableToolbarItem(
  page: Page,
  itemName: string,
  kebab = false,
) {
  const toolbar = await resolveTableToolbar(page);
  await expect(toolbar).toBeVisible({ timeout: TABLE_LOAD_TIMEOUT_MS });

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

  if (await exactToolbarItem.isVisible()) {
    await exactToolbarItem.click();
    return;
  }

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
}

export async function getTableData(page: Page, name: string) {
  const rowsLocator = await getTableRows(page, name);
  const rowCount = await rowsLocator.count();
  const tableData: string[][] = [];

  for (let rowIndex = 0; rowIndex < rowCount; rowIndex++) {
    const row = rowsLocator.nth(rowIndex);
    tableData.push(
      (await row.locator("td").allInnerTexts()).map((t) => t.trim()),
    );
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
  await table
    .locator("tbody")
    .waitFor({ state: "visible", timeout: TABLE_LOAD_TIMEOUT_MS });
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
    const rowName = row;
    let rows: string[][] = [];
    let rowIndex = -1;

    try {
      await expect
        .poll(
          async () => {
            rows = await getTableData(page, tableName);
            rowIndex = rows.findIndex((r) => r.includes(rowName));
            return rowIndex;
          },
          { timeout: TABLE_LOAD_TIMEOUT_MS },
        )
        .not.toBe(-1);
    } catch {
      throw new Error(`Row ${rowName} not found: ${JSON.stringify(rows)}`);
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
  await waitForLoadingCycle(page);
}
