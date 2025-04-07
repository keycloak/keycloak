import PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import { Page } from "@playwright/test";
import { selectItem } from "../utils/form";

export async function goToPermissions(page: Page) {
  await page.getByTestId("nav-item-permissions").click();
}

export async function goToEvaluation(page: Page) {
  await page.getByTestId("permissionsEvaluation").click();
}

export async function clickCreatePermission(page: Page) {
  await page.getByTestId("no-permissions-empty-action").click();
}

export async function selectUsersResource(page: Page) {
  await page.getByRole("gridcell", { name: "Users", exact: true }).click();
}

export async function fillUserPermissionForm(
  page: Page,
  data: PolicyRepresentation,
) {
  const entries = Object.entries(data);
  for (const [key, value] of entries) {
    if (key === "scopes") {
      await selectItem(page, "#scopes", value[0]);
      continue;
    }
    await page.getByTestId(key).fill(value);
  }
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
