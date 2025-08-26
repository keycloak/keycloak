import { type Page, expect } from "@playwright/test";
import { assertSwitchIsChecked, selectItem, switchOff } from "../utils/form.ts";
import { clickTableToolbarItem, getTableData } from "../utils/table.ts";

export async function selectClientScopeFilter(page: Page, value: string) {
  await page.getByTestId("clientScopeSearch").click();
  await page.getByRole("menuitem", { name: value, exact: true }).click();
}

export async function selectSecondaryFilterAssignedType(
  page: Page,
  value: string,
) {
  await selectItem(page, page.getByTestId("clientScopeSearchType"), value);
}

export async function selectSecondaryFilterProtocol(
  page: Page,
  protocol: string,
) {
  await selectItem(
    page,
    page.getByTestId("clientScopeSearchProtocol"),
    protocol,
  );
}

export async function getTableAssignedTypeColumn(
  page: Page,
  tableName: string,
) {
  const rows = await getTableData(page, tableName);
  return rows.map((row) => row[2]);
}

export async function getTableProtocolColumn(page: Page, tableName: string) {
  const rows = await getTableData(page, tableName);
  return rows.map((row) => row[3]);
}

export async function selectChangeType(page: Page, value: string) {
  await selectItem(
    page,
    page.getByRole("button", { name: "Change type to" }),
    value,
  );
}

export async function goToCreateItem(page: Page) {
  await clickTableToolbarItem(page, "Create client scope");
}

export async function fillClientScopeData(
  page: Page,
  name: string,
  description = "",
  consentScreenText = "",
  displayOrder = "",
) {
  await page.getByTestId("name").fill(name);
  await page.getByTestId("description").fill(description);
  await selectItem(page, "#kc-protocol", "OpenID Connect");

  await getConsentScreenTextInput(page).fill(consentScreenText);
  await page.getByTestId("attributes.gui🍺order").fill(displayOrder);
}

function getSwitchDisplayOnConsentScreenInput() {
  return "#attributes\\.display🍺on🍺consent🍺screen";
}

function getConsentScreenTextInput(page: Page) {
  return page.getByTestId("attributes.consent🍺screen🍺text");
}

export async function assertSwitchDisplayOnConsentScreenIsChecked(page: Page) {
  await assertSwitchIsChecked(page, getSwitchDisplayOnConsentScreenInput());
}

export async function assertConsentInputIsVisible(page: Page, not = false) {
  if (not) {
    await expect(getConsentScreenTextInput(page)).toBeHidden();
  } else {
    await expect(getConsentScreenTextInput(page)).toBeVisible();
  }
}

export async function switchOffDisplayOnConsentScreen(page: Page) {
  await switchOff(page, getSwitchDisplayOnConsentScreenInput());
}
