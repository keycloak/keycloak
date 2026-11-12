import { Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";

export async function clickSaveRealm(page: Page) {
  await page.getByTestId("realmSettingsGeneralTab-save").click();
}

// ---------------------------------------------------------------------------
// Allowed protocol mapper types for client administrators (multi-select)
// ---------------------------------------------------------------------------

const allowedMapperTypesSelect =
  "#attributes\\.allowedProtocolMapperTypesForClientAdministrators";

export function getAllowedMapperTypesLocator(page: Page) {
  return page.locator(allowedMapperTypesSelect);
}

export async function openAllowedMapperTypesDropdown(page: Page) {
  await getAllowedMapperTypesLocator(page).click();
}

export async function selectAllowedMapperTypes(
  page: Page,
  mapperIds: string[],
) {
  // Open the dropdown once; for typeaheadMulti it stays open after each selection.
  await openAllowedMapperTypesDropdown(page);
  for (const id of mapperIds) {
    await page.getByRole("option", { name: id, exact: true }).click();
  }
  // Close the dropdown so it doesn't obscure the Save button.
  await openAllowedMapperTypesDropdown(page);
}

export async function clearAllowedMapperTypes(page: Page) {
  // The "×" clear button inside the typeahead input clears all selections.
  await getAllowedMapperTypesLocator(page)
    .locator("button[aria-label='Clear input value']")
    .click();
}

/** Returns the text of every chip currently shown in the multi-select. */
export async function getAllowedMapperTypeChips(page: Page): Promise<string[]> {
  const toggle = getAllowedMapperTypesLocator(page);
  const chips = toggle.locator(".pf-v5-c-chip__text");
  // Wait for at least one chip to be visible before collecting all text.
  await chips.first().waitFor({ state: "visible" });
  const texts = await chips.allTextContents();
  return texts.map((t) => t.trim()).filter(Boolean);
}

function getDisplayName(page: Page) {
  return page.getByTestId("displayName");
}

export async function fillDisplayName(page: Page, value: string) {
  await getDisplayName(page).fill(value);
}

export async function assertDisplayName(page: Page, value: string) {
  await expect(getDisplayName(page)).toHaveValue(value);
}

function getFrontendURL(page: Page) {
  return page.getByTestId("attributes.frontendUrl");
}

export async function fillFrontendURL(page: Page, value: string) {
  await getFrontendURL(page).fill(value);
}

export async function assertFrontendURL(page: Page, value: string) {
  await expect(getFrontendURL(page)).toHaveValue(value);
}

export async function fillRequireSSL(page: Page, value: string) {
  await selectItem(page, "#sslRequired", value);
}

export async function assertRequireSSL(page: Page, value: string) {
  await expect(page.locator("#sslRequired")).toHaveText(value);
}

export async function clickRevertButton(page: Page) {
  await page.getByTestId("realmSettingsGeneralTab-revert").click();
}
