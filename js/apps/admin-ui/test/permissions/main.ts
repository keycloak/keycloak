import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation.js";
import type { Page } from "@playwright/test";
import { selectItem } from "../utils/form.ts";
import { confirmModal } from "../utils/modal.ts";
import { clickRowKebabItem } from "../utils/table.ts";

type PermissionForm = PolicyRepresentation & {
  enforcementMode?: "allResources" | "specificResources";
};

export async function goToPermissions(page: Page) {
  await page.getByTestId("nav-item-permissions").click();
}

export async function goToEvaluation(page: Page) {
  await page.getByTestId("permissionsEvaluation").click();
}

export async function clickCreatePermission(page: Page) {
  await page.getByTestId("no-permissions-empty-action").click();
}

export async function selectResource(page: Page, resourceName: string) {
  await page.getByRole("gridcell", { name: resourceName, exact: true }).click();
}

export async function fillPermissionForm(page: Page, data: PermissionForm) {
  const entries = Object.entries(data);
  for (const [key, value] of entries) {
    if (key === "scopes") {
      await selectItem(page, "#scopes", value[0]);
      continue;
    }
    if (key === "enforcementMode") {
      await page.locator(`input[id='${value}']`).click();
      continue;
    }
    await page.getByTestId(key).fill(value);
  }
}

export async function pickGroup(page: Page, groupName: string) {
  await page.getByTestId("select-group-button").click();
  await page.getByTestId(`${groupName}-check`).check();
  await page.getByTestId("add-button").click();
}

export async function removeGroup(page: Page, groupName: string) {
  await page
    .getByRole("row", { name: `/${groupName}` })
    .getByRole("button")
    .click();
}

export async function clickCreateNewPolicy(page: Page) {
  await page.getByTestId("select-createNewPolicy-button").click();
}

export async function clickCreatePolicySaveButton(page: Page) {
  await page
    .getByRole("dialog", { name: "Create policy" })
    .getByTestId("save")
    .click();
}

export async function openSearchPanel(page: Page) {
  await page.getByTestId("searchdropdown_dorpdown").click();
}

export async function clickSearchButton(page: Page) {
  await page.getByTestId("search-btn").click();
}

export async function deletePermission(page: Page, name: string) {
  await clickRowKebabItem(page, name, "Delete");
  await confirmModal(page);
}
