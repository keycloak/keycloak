import { expect, test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { assertModalTitle, confirmModal } from "../utils/modal.ts";
import {
  pickRoleType,
  clickHideInheritedRoles,
  clickUnassign,
  confirmModalAssign,
  pickRole,
} from "../utils/roles.ts";
import { goToClientScopes } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertRowExists,
  clickSelectRow,
  clickTableRowItem,
  getTableData,
  searchItem,
} from "../utils/table.ts";
import { goToScopeTab } from "./scope.ts";

test.describe.serial("Scope tab test", () => {
  const scopeName = `client-scope-mapper-${uuidv4()}`;
  const tableName = "Role list";

  test.beforeAll(async () =>
    adminClient.createClientScope({
      name: scopeName,
      description: "",
      protocol: "openid-connect",
    }),
  );

  test.afterAll(() => adminClient.deleteClientScope(scopeName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToClientScopes(page);
    await searchItem(page, "Search for client scope", scopeName);
    await clickTableRowItem(page, scopeName);
    await goToScopeTab(page);
  });

  test("Assign and unassign role", async ({ page }) => {
    const role = "admin";

    await pickRoleType(page, "roles");
    await pickRole(page, role, true);
    await confirmModalAssign(page);

    await assertNotificationMessage(page, "Role mapping updated");

    await assertRowExists(page, role);

    await clickHideInheritedRoles(page);
    const data = await getTableData(page, tableName);
    expect(data.length).toBeGreaterThan(1);
    await clickHideInheritedRoles(page);

    await clickSelectRow(page, tableName, role);
    await clickUnassign(page);

    await assertModalTitle(page, "Remove role?");
    await confirmModal(page);
    await assertEmptyTable(page);
  });
});
