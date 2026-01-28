import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { selectChangeType } from "../client-scope/main.ts";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { assertModalTitle, confirmModal } from "../utils/modal.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertRowExists,
  assertTableRowsLength,
  clickNextPageButton,
  clickRowKebabItem,
  clickSelectRow,
  clickTableRowItem,
  clickTableToolbarItem,
  getTableData,
  searchItem,
} from "../utils/table.ts";
import {
  assertHasAccessTokenGenerated,
  assertHasIdTokenGenerated,
  assertHasUserInfoGenerated,
  assertNoAccessTokenGenerated,
  assertNoIdTokenGenerated,
  assertNoUserInfoGenerated,
  assertTableCellDropdownValue,
  clickAddClientScope,
  clickAddScope,
  goToClientScopeEvaluateTab,
  goToClientScopesTab,
  goToGenerateAccessTokenTab,
  selectUser,
} from "./scope.ts";

type ClientScope = {
  name: string;
  description: string;
  protocol: string;
  attributes: {
    "include.in.token.scope": string;
    "display.on.consent.screen": string;
    "gui.order": string;
    "consent.screen.text": string;
  };
};

test.describe.serial("Client details - Client scopes subtab", () => {
  const clientId = "client-scopes-subtab-test";
  const clientScopeName = "client-scope-test";
  const clientScopeNameDefaultType = "client-scope-test-default-type";
  const clientScopeNameOptionalType = "client-scope-test-optional-type";
  const msgScopeMappingRemoved = "Scope mapping successfully removed";
  const realmName = `clients-realm-${uuid()}`;
  const placeHolder = "Search by name";
  const tableName = "Client scopes";

  const clientScope: ClientScope = {
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

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createClient({
      realm: realmName,
      clientId,
      protocol: "openid-connect",
      publicClient: false,
    });
    for (let i = 0; i < 5; i++) {
      clientScope.name = clientScopeName + i;
      await adminClient.createClientScope({ ...clientScope, realm: realmName });
      await adminClient.addDefaultClientScopeInClient(
        clientScopeName + i,
        clientId,
        realmName,
      );
    }
    clientScope.name = clientScopeNameDefaultType;
    await adminClient.createClientScope({ ...clientScope, realm: realmName });
    clientScope.name = clientScopeNameOptionalType;
    await adminClient.createClientScope({ ...clientScope, realm: realmName });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await goToClientScopesTab(page);
  });

  test("Should list client scopes", async ({ page }) => {
    const rows = await getTableData(page, tableName);
    expect(rows.length).toBeGreaterThan(2);
    await assertRowExists(page, clientScopeName + "0");
  });

  test("Should search existing client scope by name", async ({ page }) => {
    await searchItem(page, placeHolder, clientScopeName + "0");
    await assertRowExists(page, clientScopeName + "0");
    await assertTableRowsLength(page, tableName, 1);
  });

  test("Should search non-existent client scope by name", async ({ page }) => {
    await searchItem(page, placeHolder, "non-existent-item");
    await assertEmptyTable(page);
  });

  test("Should add client scope with optional assigned type", async ({
    page,
  }) => {
    await clickAddClientScope(page);
    await assertModalTitle(page, `Add client scopes to ${clientId}`);
    await clickSelectRow(
      page,
      "Choose a mapper type",
      clientScopeNameOptionalType,
    );
    await clickAddScope(page, "Optional");
    await assertNotificationMessage(page, "Scope mapping updated");
    await searchItem(page, placeHolder, clientScopeNameOptionalType);
    await assertTableRowsLength(page, tableName, 1);
    await assertRowExists(page, clientScopeNameOptionalType);
    await assertTableCellDropdownValue(page, "Optional");
  });

  const itemName = clientScopeName + 0;
  test(`Should change item AssignedType to default from search bar`, async ({
    page,
  }) => {
    await searchItem(page, placeHolder, itemName);
    await assertTableRowsLength(page, tableName, 1);
    await clickSelectRow(page, tableName, itemName);
    await selectChangeType(page, "Default");
    await assertNotificationMessage(page, "Scope mapping updated");
    await searchItem(page, placeHolder, itemName);
    await assertTableRowsLength(page, tableName, 1);
    await assertTableCellDropdownValue(page, "Default");
    await assertRowExists(page, itemName);
  });

  test("Should show items on next page are more than 11", async ({ page }) => {
    await clickNextPageButton(page);
    const rows = await getTableData(page, tableName);
    expect(rows.length).toBeGreaterThan(1);
  });

  test("Should remove client scope", async ({ page }) => {
    await searchItem(page, placeHolder, clientScopeName + "0");
    await clickRowKebabItem(page, clientScopeName + "0", "Remove");
    await confirmModal(page);
    await assertNotificationMessage(page, msgScopeMappingRemoved);
    await searchItem(page, placeHolder, clientScopeName + "0");
    await assertEmptyTable(page);
  });

  test("Should remove multiple client scopes from search bar", async ({
    page,
  }) => {
    const itemName1 = clientScopeName + 1;
    const itemName2 = clientScopeName + 2;
    await searchItem(page, placeHolder, clientScopeName);
    await assertTableRowsLength(page, tableName, 5);
    await clickSelectRow(page, tableName, itemName1);
    await clickSelectRow(page, tableName, itemName2);
    await clickTableToolbarItem(page, "Remove", true);
    await assertNotificationMessage(page, msgScopeMappingRemoved);
    await searchItem(page, placeHolder, clientScopeName);
    await assertTableRowsLength(page, tableName, 3);
    await assertRowExists(page, itemName1, false);
    await assertRowExists(page, itemName2, false);
  });
});

test.describe.serial("Client scopes evaluate subtab", () => {
  const clientName = "testClient";
  const userName = "admin-a";
  const realmName = `clients-realm-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createClient({
      realm: realmName,
      protocol: "openid-connect",
      clientId: clientName,
      publicClient: false,
    });
    await adminClient.createUser({
      realm: realmName,
      username: userName,
      enabled: true,
    });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
  });

  test("check effective protocol mappers list", async ({ page }) => {
    await searchItem(page, "Search for client", clientName);
    await clickTableRowItem(page, clientName);
    await goToClientScopesTab(page);
    await goToClientScopeEvaluateTab(page);

    const rows = await getTableData(page, "Effective protocol mappers");
    expect(rows.length).toBeGreaterThan(1);
  });

  test("check generated id token and user info", async ({ page }) => {
    await searchItem(page, "Search for client", clientName);
    await clickTableRowItem(page, clientName);
    await goToClientScopesTab(page);
    await goToClientScopeEvaluateTab(page);

    await assertNoAccessTokenGenerated(page);
    await assertNoIdTokenGenerated(page);
    await assertNoUserInfoGenerated(page);

    await goToGenerateAccessTokenTab(page);
    await selectUser(page, userName);

    await assertHasUserInfoGenerated(page, userName);
    await assertHasAccessTokenGenerated(page, userName);
    await assertHasIdTokenGenerated(page, userName);
  });
});
