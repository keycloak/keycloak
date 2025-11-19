import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { createTestBed } from "../support/testbed.ts";
import { login } from "../utils/login.ts";
import { goToClientScopes, goToClients, goToRealm } from "../utils/sidebar.ts";
import { clickTableToolbarItem, getRowByCellText } from "../utils/table.ts";
import { clickSaveButton, selectItem } from "../utils/form.ts";
import { toClients } from "../../src/clients/routes/Clients.tsx";
import { createClient, continueNext, save as saveClient } from "./utils.ts";

test("OIDC client can assign OID4VCI client scopes", async ({ page }) => {
  await using testBed = await createTestBed();

  await login(page, { to: toClients({ realm: testBed.realm }) });
  await goToRealm(page, testBed.realm);

  const clientScopeName = `oid4vci-scope-${uuid()}`;
  await goToClientScopes(page);
  await clickTableToolbarItem(page, "Create client scope");
  await selectItem(page, "#kc-protocol", "OpenID for Verifiable Credentials");
  await page.getByTestId("name").fill(clientScopeName);
  await clickSaveButton(page);
  await expect(page.getByText("Client scope created")).toBeVisible();

  const clientId = `oidc-client-${uuid()}`;
  await goToClients(page);
  await createClient(page, { clientId, protocol: "OpenID Connect" });
  await continueNext(page);
  await saveClient(page);
  await expect(page.getByText("Client created successfully")).toBeVisible();

  await goToClients(page);
  await expect(getRowByCellText(page, clientId)).toBeVisible();
  await getRowByCellText(page, clientId).click();

  await page.getByTestId("clientScopesTab").click();
  await page.getByTestId("clientScopesSetupTab").click();
  await page.getByRole("button", { name: "Add client scope" }).click();

  await page.getByTestId("filter-type-dropdown").click();
  await page.getByTestId("filter-type-dropdown-item").click();
  await page.locator(".kc-protocolType-select").click();
  await page
    .getByRole("option", { name: "OpenID for Verifiable Credentials" })
    .click();

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
});
