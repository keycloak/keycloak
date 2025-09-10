import { expect, test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertSaveButtonIsDisabled, clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToClientScopes } from "../utils/sidebar.ts";
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
  goToCreateItem,
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

test.describe.serial("Client Scopes test", () => {
  const clientScopeName = "client-scope-test";
  const itemId = `client-scope-test-${uuidv4()}`;
  const clientScope = {
    name: clientScopeName,
    description: "",
    protocol: "openid-connect",
    attributes: {
      "include.in.token.scope": "true",
      "display.on.consent.screen": "true",
      "gui.order": "1",
      "consent.screen.text": "",
    },
  };
  const placeHolder = "Search for client scope";
  const tableName = "Client scopes";

  test.beforeAll(async () => {
    for (let i = 0; i < 5; i++) {
      clientScope.name = clientScopeName + i;
      await adminClient.createClientScope(clientScope);
    }
  });

  test.afterAll(async () => {
    for (let i = 0; i < 5; i++) {
      if (await adminClient.existsClientScope(clientScopeName + i)) {
        await adminClient.deleteClientScope(clientScopeName + i);
      }
    }
  });

  test.describe.serial("Client Scope filter list items", () => {
    test.beforeEach(async ({ page }) => {
      await login(page);
      await goToClientScopes(page);
    });

    test("should filter item by name", async ({ page }) => {
      const itemName = clientScopeName + "0";
      await searchItem(page, placeHolder, itemName);
      await assertRowExists(page, itemName);
      await assertTableRowsLength(page, tableName, 1);
    });

    test("should filter items by Assigned type Default", async ({ page }) => {
      await selectClientScopeFilter(page, "Assigned type");
      await selectSecondaryFilterAssignedType(page, FilterAssignedType.Default);

      const assignedTypes = await getTableAssignedTypeColumn(page, tableName);

      expect(assignedTypes).toContain(FilterAssignedType.Default);
      expect(assignedTypes).not.toContain(FilterAssignedType.Optional);
      expect(assignedTypes).not.toContain(FilterAssignedType.None);
    });

    test("should filter items by Assigned type Optional", async ({ page }) => {
      await selectClientScopeFilter(page, "Assigned type");
      await selectSecondaryFilterAssignedType(
        page,
        FilterAssignedType.Optional,
      );

      const assignedTypes = await getTableAssignedTypeColumn(page, tableName);

      expect(assignedTypes).not.toContain(FilterAssignedType.Default);
      expect(assignedTypes).not.toContain(FilterAssignedType.None);
      expect(assignedTypes).toContain(FilterAssignedType.Optional);
    });

    test("should filter items by Assigned type All", async ({ page }) => {
      await selectClientScopeFilter(page, "Assigned type");
      await selectSecondaryFilterAssignedType(
        page,
        FilterAssignedType.AllTypes,
      );

      const assignedTypes = await getTableAssignedTypeColumn(page, tableName);

      expect(assignedTypes).toContain(FilterAssignedType.Default);
      expect(assignedTypes).toContain(FilterAssignedType.Optional);
      expect(assignedTypes).toContain(FilterAssignedType.None);
    });

    test("should filter items by Protocol OpenID", async ({ page }) => {
      await selectClientScopeFilter(page, "Protocol");
      await selectSecondaryFilterProtocol(page, FilterProtocol.OpenID);

      const protocols = await getTableProtocolColumn(page, tableName);

      expect(protocols).not.toContain(FilterProtocol.SAML);
      expect(protocols).toContain(FilterProtocol.OpenID);
    });

    test("should filter items by Protocol SAML", async ({ page }) => {
      await selectClientScopeFilter(page, "Protocol");
      await selectSecondaryFilterProtocol(page, FilterProtocol.SAML);

      const protocols = await getTableProtocolColumn(page, tableName);

      expect(protocols).toContain(FilterProtocol.SAML);
      expect(protocols).not.toContain(FilterProtocol.OpenID);
    });

    test("should show items on next page are more than 11", async ({
      page,
    }) => {
      await clickNextPageButton(page);
      const rows = await getTableData(page, tableName);
      expect(rows.length).toBeGreaterThan(1);
    });
  });

  test.describe.serial("Client Scope modify list items", () => {
    test.beforeEach(async ({ page }) => {
      await login(page);
      await goToClientScopes(page);
    });

    test("should modify selected item type to Default", async ({ page }) => {
      const itemName = clientScopeName + "0";
      await clickSelectRow(page, tableName, itemName);
      await selectChangeType(page, FilterAssignedType.Default);
      await assertNotificationMessage(page, "Scope mapping updated");

      const rows = await getTableData(page, tableName);
      const itemRow = rows.find((r) => r.includes(itemName));
      expect(itemRow).toContain(FilterAssignedType.Default);
    });
  });

  test.describe.serial("Client Scope creation", () => {
    test.beforeEach(async ({ page }) => {
      await login(page);
      await goToClientScopes(page);
    });

    test("should fail creating client scope", async ({ page }) => {
      await goToCreateItem(page);
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
      await goToCreateItem(page);

      await assertSwitchDisplayOnConsentScreenIsChecked(page);
      await assertConsentInputIsVisible(page);

      await switchOffDisplayOnConsentScreen(page);

      await assertConsentInputIsVisible(page, true);
    });

    test("Client scope CRUD test", async ({ page }) => {
      await assertRowExists(page, itemId, false);
      await goToCreateItem(page);

      await fillClientScopeData(page, itemId);
      await clickSaveButton(page);

      await assertNotificationMessage(page, "Client scope created");

      await goToClientScopes(page);

      await searchItem(page, placeHolder, itemId);
      await assertRowExists(page, itemId);
      await clickRowKebabItem(page, itemId, "Delete");

      await confirmModal(page);
      await assertNotificationMessage(
        page,
        "The client scope has been deleted",
      );
      await assertRowExists(page, itemId, false);
    });
  });
});
