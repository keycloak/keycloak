import { expect, test, type Page } from "@playwright/test";
import { clickSaveButton, selectItem } from "../utils/form.ts";
import { clickTableRowItem, getRowByCellText } from "../utils/table.ts";
import adminClient from "../utils/AdminClient.ts";

const OID4VCI_SERVER_FEATURE = "OID4VC_VCI";
const OID4VCI_PROTOCOL = "OpenID for Verifiable Credentials";
const OID4VCI_UNAVAILABLE_MESSAGE =
  "OID4VCI protocol is unavailable. Start Keycloak with verifiable credentials support enabled.";

export async function skipIfOID4VCIFeatureDisabled() {
  const isOID4VCIFeatureEnabled = await adminClient.isFeatureEnabled(
    OID4VCI_SERVER_FEATURE,
  );
  // eslint-disable-next-line playwright/no-skipped-test -- Explicit environment gate for unsupported server features.
  test.skip(!isOID4VCIFeatureEnabled, OID4VCI_UNAVAILABLE_MESSAGE);
}

export async function createOid4vciClientScope(
  page: Page,
  clientScopeName: string,
) {
  await skipIfOID4VCIFeatureDisabled();
  await clickCreateClientScopeAction(page);
  await selectItem(page, "#kc-protocol", OID4VCI_PROTOCOL);
  await page.getByTestId("name").fill(clientScopeName);
  await clickSaveButton(page);
  await expect(page.getByText("Client scope created")).toBeVisible();
}

export async function openClientScopeSetupTab(page: Page, clientId: string) {
  await expect(getRowByCellText(page, clientId)).toBeVisible();
  await clickTableRowItem(page, clientId);
  await page.getByTestId("clientScopesTab").click();
  await page.getByTestId("clientScopesSetupTab").click();
}

export async function assignOptionalOid4vciClientScope(
  page: Page,
  clientScopeName: string,
) {
  await page.getByRole("button", { name: "Add client scope" }).click();
  await page.getByTestId("filter-type-dropdown").click();
  await page.getByTestId("filter-type-dropdown-item").click();
  await page.locator(".kc-protocolType-select").click();
  await page.getByRole("option", { name: OID4VCI_PROTOCOL }).click();

  await expect(
    page.getByRole("gridcell", { name: clientScopeName }),
  ).toBeVisible();

  const scopeRow = page.getByRole("row", { name: clientScopeName });
  await scopeRow.getByRole("checkbox").click();

  await page.getByTestId("add-dropdown").click();
  await page.getByRole("menuitem", { name: "Optional" }).click();

  await expect(page.getByText("Scope mapping updated")).toBeVisible();
  await expect(
    page.getByRole("row", { name: new RegExp(clientScopeName, "i") }),
  ).toBeVisible();
}

async function clickCreateClientScopeAction(page: Page) {
  const createScopeButton = page.getByRole("button", {
    name: /Create client scope/i,
  });
  if ((await createScopeButton.count()) > 0) {
    await createScopeButton.first().click();
    return;
  }

  await page
    .getByRole("link", { name: /Create client scope/i })
    .first()
    .click();
}
