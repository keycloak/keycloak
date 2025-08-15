import { type Page, expect } from "@playwright/test";
import {
  assertEmptyTable,
  assertRowExists,
  searchItem,
} from "../utils/table.ts";
import { confirmModal } from "../utils/modal.ts";

export async function goToClientPoliciesTab(page: Page) {
  await page.getByTestId("rs-clientPolicies-tab").click();
}

export async function goToClientProfilesList(page: Page) {
  await page.getByTestId("rs-policies-clientProfiles-tab").click();
}

export async function shouldDisplayProfilesTab(page: Page) {
  await expect(page.getByTestId("createProfileBtn")).toBeVisible();
  await expect(page.getByTestId("formViewSelect")).toBeVisible();
  await expect(page.getByTestId("jsonEditorSelect")).toBeVisible();
  await expect(page.locator("table")).toBeVisible();
  await expect(page.locator("td")).toContainText("Global");
}

export async function createClientProfile(
  page: Page,
  name: string,
  description: string,
) {
  await page.getByTestId("createProfile").click();
  await page.getByTestId("name").fill(name);
  await page.getByTestId("description").fill(description);
}

export async function saveClientProfileCreation(page: Page) {
  await page.getByTestId("saveCreateProfile").click();
}

export async function cancelClientProfileCreation(page: Page) {
  await page.getByTestId("cancelCreateProfile").click();
}

export async function searchClientProfile(page: Page, name: string) {
  await searchItem(page, "Search", name);
  await assertRowExists(page, name);
}

export async function checkElementNotInList(page: Page, name: string) {
  await assertRowExists(page, name, false);
}

export async function searchNonExistingClientProfile(page: Page, name: string) {
  await searchItem(page, "Search", name);
  await assertEmptyTable(page);
}

export async function clickAddExecutor(page: Page) {
  await page.getByTestId("addExecutor").click();
}

export async function selectExecutorType(page: Page, type: string) {
  await page.locator("#kc-executor").click();
  await page.getByTestId(type).click();
}

export async function assertIntentClient(page: Page, value: string) {
  await expect(
    page.getByTestId("intent-client-bind-check-endpoint"),
  ).toHaveValue(value);
}

export async function clickSaveClientProfile(page: Page) {
  await page.getByTestId("addExecutor-saveBtn").click();
}

export async function assertExecutorInList(page: Page, name: string) {
  await expect(page.getByText(name)).toBeVisible();
}

export async function clickDeleteExecutor(page: Page, name: string) {
  await page.getByTestId(`deleteExecutor-${name}`).click();
  await confirmModal(page);
}
