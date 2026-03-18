import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation.js";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation.js";
import { type Page, expect } from "@playwright/test";
import { clickRowKebabItem, getRowByCellText } from "../utils/table.ts";
import { confirmModal } from "../utils/modal.ts";
import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation.js";

export async function goToAuthorizationTab(page: Page) {
  await page.getByTestId("authorizationTab").click();
}

export async function goToResourcesSubTab(page: Page) {
  await page.getByTestId("authorizationResources").click();
}

export async function setPolicy(page: Page, policy: string) {
  await page.getByTestId(policy).click();
}

export async function clickAuthenticationSaveButton(page: Page) {
  await page.getByTestId("authenticationSettings-save").click();
}

export async function assertResource(page: Page, name: string) {
  await expect(getRowByCellText(page, name)).toBeVisible();
}

export async function createResource(
  page: Page,
  resource: ResourceRepresentation,
) {
  await page
    .locator(
      '[data-testid="createResource"], [data-testid="no-resources-empty-action"]',
    )
    .click();
  await fillForm(page, resource);
}

export async function fillForm(
  page: Page,
  resource:
    | ResourceRepresentation
    | ScopeRepresentation
    | PolicyRepresentation
    | { [key: string]: string },
) {
  for (const [key, value] of Object.entries(resource)) {
    if (Array.isArray(value)) {
      for (let index = 0; index < value.length; index++) {
        const v = value[index];
        await page.getByTestId(`${key}${index}`).fill(v);
        await page.getByTestId("uris-addValue").click();
      }
    } else {
      await page.getByTestId(key).fill(value);
    }
  }
}

export async function goToScopesSubTab(page: Page) {
  await page.getByTestId("authorizationScopes").click();
}

export async function createAuthorizationScope(
  page: Page,
  scope: ScopeRepresentation,
) {
  await page.getByTestId("no-authorization-scopes-empty-action").click();
  await fillForm(page, scope);
}

export async function goToPoliciesSubTab(page: Page) {
  await page.getByTestId("authorizationPolicies").click();
}

export async function createPolicy(
  page: Page,
  type: string,
  policy: { [key: string]: string },
) {
  await page
    .locator(
      '[data-testid="createPolicy"], [data-testid="no-policies-empty-action"]',
    )
    .click();
  await page.getByRole("gridcell", { name: type, exact: true }).click();
  await fillForm(page, policy);
}

export async function deletePolicy(page: Page, policyName: string) {
  await clickRowKebabItem(page, policyName, "Delete");
  await confirmModal(page);
}

export async function inputClient(page: Page, clientName: string) {
  await page.getByLabel("Type to filter").click();
  await page.getByLabel("Type to filter").fill(clientName);
  await page.getByRole("option", { name: clientName }).click();
}

export async function goToPermissionsSubTab(page: Page) {
  await page.getByTestId("authorizationPermissions").click();
}

export async function createPermission(
  page: Page,
  type: string,
  permission: PolicyRepresentation,
) {
  const dropdown = page.getByTestId("permissionCreateDropdown");
  const hasDropdown = (await dropdown.count()) > 0;

  if (hasDropdown) {
    await dropdown.click();
  }

  await page.getByTestId(`create-${type}`).click();
  await fillForm(page, permission);
}

export async function selectResource(page: Page, resourceName: string) {
  await page.locator("#resources").getByLabel("Menu toggle").click();
  await page.getByRole("option", { name: resourceName }).click();
}

export async function goToExportSubTab(page: Page) {
  await page.getByTestId("authorizationExport").click();
}

export async function clickCopyButton(page: Page) {
  await page.getByTestId("authorization-export-copy").click();
}

export async function assertClipboardHasText(page: Page) {
  const clipboardText = await page.evaluateHandle(() =>
    navigator.clipboard.readText(),
  );
  await expect(page.getByTestId("authorization-export-code-editor")).toHaveText(
    await clipboardText.jsonValue(),
  );
}

export async function assertDownload(page: Page) {
  const downloadPromise = page.waitForEvent("download");

  await page.getByTestId("authorization-export-download").click();

  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBeDefined();
}
