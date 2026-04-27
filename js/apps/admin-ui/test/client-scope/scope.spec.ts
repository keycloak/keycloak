import { expect, test } from "@playwright/test";
import { toClientScopes } from "../../src/client-scopes/routes/ClientScopes.tsx";
import { createTestBed } from "../support/testbed.ts";
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
import {
  assertEmptyTable,
  assertRowExists,
  clickSelectRow,
  clickTableRowItem,
  getTableData,
  searchItem,
} from "../utils/table.ts";
import { goToScopeTab } from "./scope.ts";

test.describe("Scope tab", () => {
  const tableName = "Role list";

  test("assigns and unassigns role", async ({ page }) => {
    await using testBed = await createTestBed({
      clientScopes: [
        {
          name: "test-scope",
          protocol: "openid-connect",
        },
      ],
      roles: {
        realm: [
          {
            name: "composite-role",
            composite: true,
            composites: {
              realm: ["offline_access"],
            },
          },
        ],
      },
    });

    const role = "composite-role";

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await searchItem(page, "Search for client scope", "test-scope");
    await clickTableRowItem(page, "test-scope");
    await goToScopeTab(page);

    await pickRoleType(page, "roles");
    await pickRole(page, role, true);
    await confirmModalAssign(page);

    await assertNotificationMessage(page, "Role mapping updated");

    await assertRowExists(page, role);

    // Initially, only directly assigned roles are shown (inherited roles are hidden by default)
    const directRolesOnly = await getTableData(page, tableName);
    expect(directRolesOnly.length).toBe(1); // Only composite-role is directly assigned

    // Click to show inherited roles (the toggle reveals inherited roles from composite)
    await clickHideInheritedRoles(page);
    const allRoles = await getTableData(page, tableName);
    expect(allRoles.length).toBe(2); // composite-role + offline_access (inherited from composite)

    // Click again to hide inherited roles
    await clickHideInheritedRoles(page);

    await clickSelectRow(page, tableName, role);
    await clickUnassign(page);

    await assertModalTitle(page, "Remove role?");
    await confirmModal(page);
    await assertEmptyTable(page);
  });
});
