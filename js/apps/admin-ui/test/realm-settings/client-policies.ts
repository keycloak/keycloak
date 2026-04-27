import { type Page, expect } from "@playwright/test";
import { clickRowKebabItem } from "../utils/table.ts";
import { selectItem } from "../utils/form.ts";

export async function goToClientPoliciesTab(page: Page) {
  await page.getByTestId("rs-clientPolicies-tab").click();
}

export async function goToClientPoliciesList(page: Page) {
  await page.getByTestId("rs-policies-clientPolicies-tab").click();
}

export async function goBackToPolicies(page: Page) {
  await page.getByRole("link", { name: "Client policies" }).click();
}

export async function checkDisplayPoliciesTab(page: Page) {
  await expect(page.getByTestId("rs-client-policies-tab")).toBeVisible();
}

function getPolicyNameField(page: Page) {
  return page.getByTestId("name");
}

function getPolicyDescriptionField(page: Page) {
  return page.getByTestId("client-policy-description");
}

export async function fillClientPolicyForm(
  page: Page,
  name: string,
  description: string,
) {
  await getPolicyNameField(page).fill(name);
  await getPolicyDescriptionField(page).fill(description);
}

export async function createNewClientPolicyFromEmptyState(
  page: Page,
  name: string,
  description: string,
  cancel: boolean = false,
) {
  await page.getByTestId("no-client-policies-empty-action").click();
  await fillClientPolicyForm(page, name, description);
  if (cancel) {
    await page.getByTestId("cancelCreatePolicy").click();
  }
}

export async function clickSaveClientPolicy(page: Page) {
  await page.getByTestId("saveCreatePolicy").click();
}

export async function checkNewClientPolicyForm(page: Page) {
  await expect(getPolicyNameField(page)).toBeVisible();
  await expect(getPolicyDescriptionField(page)).toBeVisible();
}

export async function clickCancelClientPolicyCreation(page: Page) {
  await page.getByTestId("cancelCreatePolicy").click();
}

export async function searchClientPolicy(page: Page, name: string) {
  await page.getByPlaceholder("Search by name").fill(name);
  await page.keyboard.press("Enter");
}

export async function shouldNotHaveConditionsConfigured(page: Page) {
  await expect(page.getByTestId("no-conditions")).toBeVisible();
}

export async function clickAddCondition(page: Page) {
  await page.getByTestId("addCondition").click();
}

async function selectConditionType(page: Page, condition: string) {
  await page.locator("#provider").click();
  await page.getByTestId(condition).click();
}

export async function clickCancelConditionButton(page: Page) {
  await page.getByTestId("addCondition-cancelBtn").click();
}

export async function shouldCancelAddingCondition(page: Page) {
  await selectConditionType(page, "any-client");
  await clickCancelConditionButton(page);
}

export async function clickSaveConditionButton(page: Page) {
  await page.getByTestId("addCondition-saveBtn").click();
}

export async function selectCondition(page: Page, condition: string) {
  await page.getByTestId(`${condition}-condition-link`).click();
}

export async function assertRoles(page: Page, role: string) {
  await expect(page.getByTestId("config.roles0")).toHaveValue(role);
}

export async function addClientScopeCondition(
  page: Page,
  scope: string = "Optional",
) {
  await selectConditionType(page, "client-scopes");
  await selectItem(page, "#type", scope);
}

export async function addClientUpdaterSourceHost(page: Page) {
  await selectConditionType(page, "client-updater-source-host");
}

export async function addClientRolesCondition(page: Page, role: string) {
  await selectConditionType(page, "client-roles");
  await fillClientRolesCondition(page, role);
}

export async function fillClientRolesCondition(page: Page, role: string) {
  await page.getByTestId("config.roles0").fill(role);
}

export async function assertExists(page: Page, row: string, exist = true) {
  await expect(page.getByTestId(row)).toBeVisible({ visible: exist });
}

export async function shouldEditClientRolesCondition(page: Page) {
  await page.getByTestId("edit-condition").click();
  await page.getByTestId("client-roles").click();
  await page.getByTestId("save").click();
}

export async function shouldEditClientScopesCondition(page: Page) {
  await page.getByTestId("edit-condition").click();
  await page.getByTestId("client-scopes").click();
  await page.getByTestId("save").click();
}

export async function deleteCondition(page: Page, name: string) {
  await page.getByTestId(`delete-${name}-condition`).click();
}

export async function checkConditionsListContains(
  page: Page,
  condition: string,
) {
  await expect(page.getByTestId("conditions-list")).toContainText(condition);
}

export async function shouldDeleteClientScopesCondition(page: Page) {
  await page.getByTestId("delete-condition").click();
  await page.getByTestId("confirm").click();
}

export async function deleteClientPolicyItemFromTable(
  page: Page,
  name: string,
) {
  await clickRowKebabItem(page, name, "Delete");
}

export async function checkElementInList(page: Page, name: string) {
  await expect(page.getByRole("row", { name })).toBeVisible();
}

export async function deleteClientPolicyFromDetails(page: Page, name: string) {
  await clickRowKebabItem(page, name, "Delete");
}

export async function shouldReloadJSONPolicies(page: Page) {
  await page.getByTestId("jsonEditor-profilesView").click();
  await page.getByTestId("jsonEditor-reloadBtn").click();
}
