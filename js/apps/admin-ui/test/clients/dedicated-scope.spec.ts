import { expect, test, type Page } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import {
  clickHideInheritedRoles,
  clickUnassign,
  confirmModalAssign,
  pickRole,
  pickRoleType,
} from "../utils/roles.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickTableRowItem,
  getTableData,
  searchItem,
} from "../utils/table.ts";
import { goToClientScopesTab } from "./scope.ts";

test.describe.serial("Client details - Dedicated scope", () => {
  const realmName = `dedicated-scope-${uuid()}`;
  const clientId = `dedicated-scope-client-${uuid()}`;
  const otherClientId = `dedicated-scope-other-${uuid()}`;
  const realmRoleName = "dedicated-realm-role";
  const compositeRoleName = "dedicated-composite-role";
  const childRoleName = "dedicated-child-role";
  const clientRoleName = "dedicated-client-role";
  const roleTable = "Role list";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createClient({
      realm: realmName,
      clientId,
      protocol: "openid-connect",
      publicClient: false,
      fullScopeAllowed: false,
    });
    const otherClient = await adminClient.createClient({
      realm: realmName,
      clientId: otherClientId,
      protocol: "openid-connect",
    });
    await adminClient.createClientRole(otherClient.id, {
      realm: realmName,
      name: clientRoleName,
    });
    await adminClient.createRealmRole({
      realm: realmName,
      name: realmRoleName,
    });
    await adminClient.createRealmRole({
      realm: realmName,
      name: childRoleName,
    });
    await adminClient.createRealmRole({
      realm: realmName,
      name: compositeRoleName,
      composite: true,
      composites: { realm: [childRoleName] },
    });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  async function goToDedicatedScopeTab(page: Page) {
    await goToRealm(page, realmName);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await goToClientScopesTab(page);
    await clickTableRowItem(page, `${clientId}-dedicated`);
    await page.getByTestId("scopeTab").click();
  }

  test("assigns realm and client roles", async ({ page }) => {
    await login(page);
    await goToDedicatedScopeTab(page);

    await pickRoleType(page, "roles");
    await pickRole(page, compositeRoleName, true);
    await confirmModalAssign(page);
    await assertNotificationMessage(page, "Scope mapping updated");
    await assertRowExists(page, compositeRoleName);

    await pickRoleType(page, "client");
    await pickRole(page, clientRoleName, true);
    await confirmModalAssign(page);
    await assertNotificationMessage(page, "Scope mapping updated");
    await assertRowExists(page, clientRoleName);
    await assertRowExists(page, otherClientId);
  });

  test("hides and shows inherited roles", async ({ page }) => {
    await login(page);
    await goToDedicatedScopeTab(page);

    const rowsFor = async (name: string) =>
      (await getTableData(page, roleTable)).filter((row) => row.includes(name));

    await expect.poll(() => rowsFor(childRoleName)).toHaveLength(0);
    await clickHideInheritedRoles(page);
    await expect
      .poll(async () => (await rowsFor(childRoleName)).map((row) => row[2]))
      .toEqual(["True"]);
    await expect
      .poll(async () => (await rowsFor(compositeRoleName)).map((row) => row[2]))
      .toEqual(["False"]);
  });

  test("does not offer already mapped roles", async ({ page }) => {
    await login(page);
    await goToDedicatedScopeTab(page);

    await pickRoleType(page, "roles");
    await assertRowExists(page, realmRoleName);
    await expect(
      page.getByRole("row", { name: new RegExp(compositeRoleName) }),
    ).toHaveCount(0);
  });

  test("unassigns a role", async ({ page }) => {
    await login(page);
    await goToDedicatedScopeTab(page);

    await page
      .getByRole("row", { name: new RegExp(clientRoleName) })
      .getByRole("checkbox")
      .check();
    await clickUnassign(page);
    await confirmModal(page);
    await assertNotificationMessage(page, "Role mapping updated");
    await expect(
      page.getByRole("row", { name: new RegExp(clientRoleName) }),
    ).toHaveCount(0);
    await assertRowExists(page, compositeRoleName);
  });
});
