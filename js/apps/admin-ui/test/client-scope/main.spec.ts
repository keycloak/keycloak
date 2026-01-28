import { expect, test } from "@playwright/test";
import { toClientScopes } from "../../src/client-scopes/routes/ClientScopes.tsx";
import { toNewClientScope } from "../../src/client-scopes/routes/NewClientScope.tsx";
import { createTestBed } from "../support/testbed.ts";
import { assertSaveButtonIsDisabled, clickSaveButton } from "../utils/form.ts";
import { login, navigateTo } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import {
  assertRowExists,
  assertTableRowsLength,
  clickNextPageButton,
  clickRowKebabItem,
  clickSelectRow,
  getTableData,
  searchItem,
} from "../utils/table.ts";
import {
  assertConsentInputIsVisible,
  assertSwitchDisplayOnConsentScreenIsChecked,
  fillClientScopeData,
  getTableAssignedTypeColumn,
  getTableProtocolColumn,
  selectChangeType,
  selectClientScopeFilter,
  selectSecondaryFilterAssignedType,
  selectSecondaryFilterProtocol,
  switchOffDisplayOnConsentScreen,
} from "./main.ts";

const FilterAssignedType = {
  AllTypes: "All types",
  Default: "Default",
  Optional: "Optional",
  None: "None",
};

const FilterProtocol = {
  All: "All",
  SAML: "SAML",
  OpenID: "OpenID Connect",
};

const placeHolder = "Search for client scope";
const tableName = "Client scopes";

test.describe("Client scopes filtering", () => {
  test("filters item by name", async ({ page }) => {
    await using testBed = await createTestBed({
      clientScopes: [{ name: "test-scope" }],
    });

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await searchItem(page, placeHolder, "test-scope");
    await assertRowExists(page, "test-scope");
    await assertTableRowsLength(page, tableName, 1);
  });

  test("filters items by assigned type 'default'", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await selectClientScopeFilter(page, "Assigned type");
    await selectSecondaryFilterAssignedType(page, FilterAssignedType.Default);

    const assignedTypes = await getTableAssignedTypeColumn(page, tableName);

    expect(assignedTypes).toContain(FilterAssignedType.Default);
    expect(assignedTypes).not.toContain(FilterAssignedType.Optional);
    expect(assignedTypes).not.toContain(FilterAssignedType.None);
  });

  test("filters items by assigned type 'optional'", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await selectClientScopeFilter(page, "Assigned type");
    await selectSecondaryFilterAssignedType(page, FilterAssignedType.Optional);

    const assignedTypes = await getTableAssignedTypeColumn(page, tableName);

    expect(assignedTypes).not.toContain(FilterAssignedType.Default);
    expect(assignedTypes).not.toContain(FilterAssignedType.None);
    expect(assignedTypes).toContain(FilterAssignedType.Optional);
  });

  test("filters items by assigned type 'all'", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await selectClientScopeFilter(page, "Assigned type");
    await selectSecondaryFilterAssignedType(page, FilterAssignedType.AllTypes);

    const assignedTypes = await getTableAssignedTypeColumn(page, tableName);

    expect(assignedTypes).toContain(FilterAssignedType.Default);
    expect(assignedTypes).toContain(FilterAssignedType.Optional);
  });

  test("filters items by protocol 'openid'", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await selectClientScopeFilter(page, "Protocol");
    await selectSecondaryFilterProtocol(page, FilterProtocol.OpenID);

    const protocols = await getTableProtocolColumn(page, tableName);

    expect(protocols).not.toContain(FilterProtocol.SAML);
    expect(protocols).toContain(FilterProtocol.OpenID);
  });

  test("filters items by protocol 'saml'", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await selectClientScopeFilter(page, "Protocol");
    await selectSecondaryFilterProtocol(page, FilterProtocol.SAML);

    const protocols = await getTableProtocolColumn(page, tableName);

    expect(protocols).toContain(FilterProtocol.SAML);
    expect(protocols).not.toContain(FilterProtocol.OpenID);
  });

  test("shows items on next page are more than 11", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await clickNextPageButton(page);
    const rows = await getTableData(page, tableName);
    expect(rows.length).toBeGreaterThan(1);
  });
});

test.describe("Client scopes modification", () => {
  test("modifies selected item type to 'default'", async ({ page }) => {
    await using testBed = await createTestBed({
      clientScopes: [{ name: "test-scope" }],
    });

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await clickSelectRow(page, tableName, "test-scope");
    await selectChangeType(page, FilterAssignedType.Default);
    await assertNotificationMessage(page, "Scope mapping updated");

    const rows = await getTableData(page, tableName);
    const itemRow = rows.find((r) => r.includes("test-scope"));
    expect(itemRow).toContain(FilterAssignedType.Default);
  });
});

test.describe("Client scopes creation", () => {
  test("fails creating client scope with an existing name", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toNewClientScope({ realm: testBed.realm }) });

    await assertSaveButtonIsDisabled(page);

    await fillClientScopeData(page, "address");
    await page.getByTestId("save").click();

    await assertNotificationMessage(
      page,
      "Could not create client scope: 'Client Scope address already exists'",
    );

    await fillClientScopeData(page, "");
    await expect(page.getByTestId("save")).toBeDisabled();
  });

  test("hides 'consent text' field when 'display consent' switch is disabled", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toNewClientScope({ realm: testBed.realm }) });

    await assertSwitchDisplayOnConsentScreenIsChecked(page);
    await assertConsentInputIsVisible(page);

    await switchOffDisplayOnConsentScreen(page);

    await assertConsentInputIsVisible(page, true);
  });

  test("creates and deletes a client scope", async ({ page }) => {
    const itemId = "test-scope";
    await using testBed = await createTestBed();

    await login(page, { to: toNewClientScope({ realm: testBed.realm }) });
    await fillClientScopeData(page, itemId);
    await clickSaveButton(page);

    await assertNotificationMessage(page, "Client scope created");

    await navigateTo(page, toClientScopes({ realm: testBed.realm }));

    await searchItem(page, placeHolder, itemId);
    await assertRowExists(page, itemId);
    await clickRowKebabItem(page, itemId, "Delete");

    await confirmModal(page);
    await assertNotificationMessage(page, "The client scope has been deleted");
    await assertRowExists(page, itemId, false);
  });
});
