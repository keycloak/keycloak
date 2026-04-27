import { Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";

export async function clickSaveRealm(page: Page) {
  await page.getByTestId("realmSettingsGeneralTab-save").click();
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
