import { type Page, expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { login } from "../utils/login.ts";
import {
  goToClientScopes,
  goToClients,
  goToGroups,
  goToIdentityProviders,
  goToOrganizations,
  goToRealm,
  goToRealmRoles,
  goToUsers,
} from "../utils/sidebar.ts";
import { clickTableRowItem, searchItem } from "../utils/table.ts";

const oidcConfig = {
  authorizationUrl: "http://localhost/auth",
  tokenUrl: "http://localhost/token",
  clientId: "client",
};

const hardcodedClaimMapper = {
  name: "mapper",
  protocol: "openid-connect",
  protocolMapper: "oidc-hardcoded-claim-mapper",
  config: { "claim.name": "claim", "claim.value": "value" },
};

// The "Admin events" tab of an object lists the events recorded for its resource path and for its sub-resources.
test.describe("Admin events tab of resources", () => {
  const realmNames: string[] = [];

  // Every test works in a realm of its own, so tests don't see each other's events.
  async function createRealm(eventsEnabled = false) {
    const realmName = `resource-events-${uuid()}`;
    realmNames.push(realmName);
    await adminClient.createRealm(realmName, {
      enabled: true,
      adminEventsEnabled: true,
      eventsEnabled,
      organizationsEnabled: true,
    });
    return realmName;
  }

  test.afterAll(async () => {
    for (const realmName of realmNames) {
      await adminClient.deleteRealm(realmName);
    }
  });

  async function openRealm(page: Page, realmName: string) {
    await login(page);
    await goToRealm(page, realmName);
  }

  // The resource path column is truncated, so the events are identified by the resource type and the operation type.
  // The number of rows ensures that events of other resources are not listed.
  async function assertEvents(page: Page, events: string[]) {
    // Rows are found by role, as expandable tables also contain hidden detail rows.
    const rows = page
      .getByRole("row")
      .filter({ hasText: /(CREATE|UPDATE|DELETE|ACTION)/ });
    await expect(rows).toHaveCount(events.length);
    for (const event of events) {
      await expect(
        rows
          .filter({ hasText: new RegExp(event.replace(" ", "\\s*")) })
          .first(),
        `Event ${event}`,
      ).toBeVisible();
    }
  }

  test("Client shows its own and its sub-resource events", async ({ page }) => {
    const realmName = await createRealm();
    const clientId = `client-${uuid()}`;
    const { id } = await adminClient.createClient({
      clientId,
      realm: realmName,
    });
    await adminClient.addClientProtocolMapper(
      id,
      hardcodedClaimMapper,
      realmName,
    );

    await openRealm(page, realmName);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await page.getByTestId("events-tab").click();
    await page.getByRole("tab", { name: "Admin events" }).click();

    await assertEvents(page, ["CLIENT CREATE", "PROTOCOL_MAPPER CREATE"]);
  });

  test("Client scope shows its own and its mapper events", async ({ page }) => {
    const realmName = await createRealm();
    const name = `scope-${uuid()}`;
    const { id } = await adminClient.createClientScope({
      name,
      protocol: "openid-connect",
      realm: realmName,
    });
    await adminClient.addClientScopeProtocolMapper(
      id!,
      hardcodedClaimMapper,
      realmName,
    );

    await openRealm(page, realmName);
    await goToClientScopes(page);
    await searchItem(page, "Search for client scope", name);
    await clickTableRowItem(page, name);
    await page.getByTestId("admin-events-tab").click();

    await assertEvents(page, ["CLIENT_SCOPE CREATE", "PROTOCOL_MAPPER CREATE"]);
  });

  test("Realm role shows its update events", async ({ page }) => {
    const realmName = await createRealm();
    const roleName = `role-${uuid()}`;
    await adminClient.createRealmRole({ name: roleName, realm: realmName });
    const role = await adminClient.getRealmRole(roleName, realmName);
    await adminClient.updateRealmRoleById(
      role!.id!,
      { ...role, description: "updated" },
      realmName,
    );

    await openRealm(page, realmName);
    await goToRealmRoles(page);
    await searchItem(page, "Search role by name", roleName);
    await clickTableRowItem(page, roleName);
    await page.getByTestId("admin-events-tab").click();

    await assertEvents(page, ["REALM_ROLE UPDATE"]);
  });

  test("Client role shows only events of its own name", async ({ page }) => {
    const realmName = await createRealm();
    const clientId = `client-${uuid()}`;
    const { id } = await adminClient.createClient({
      clientId,
      realm: realmName,
    });
    await adminClient.createClientRole(id, { name: "admin", realm: realmName });
    // A role whose name starts with the same text must not show up in the events of "admin".
    await adminClient.createClientRole(id, {
      name: "admin-extra",
      realm: realmName,
    });

    await openRealm(page, realmName);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await page.getByTestId("rolesTab").click();
    await clickTableRowItem(page, "admin");
    await page.getByTestId("admin-events-tab").click();

    await assertEvents(page, ["CLIENT_ROLE CREATE"]);
  });

  test("Client scope shows events of changing the realm default scopes", async ({
    page,
  }) => {
    const realmName = await createRealm();
    const name = `scope-${uuid()}`;
    const { id } = await adminClient.createClientScope({
      name,
      protocol: "openid-connect",
      realm: realmName,
    });
    await adminClient.addRealmDefaultClientScope(id!, realmName);

    await openRealm(page, realmName);
    await goToClientScopes(page);
    await searchItem(page, "Search for client scope", name);
    await clickTableRowItem(page, name);
    await page.getByTestId("admin-events-tab").click();

    await assertEvents(page, ["CLIENT_SCOPE CREATE", "CLIENT_SCOPE CREATE"]);
  });

  test("Client role treats special characters of its name literally and loads when opened directly", async ({
    page,
  }) => {
    const realmName = await createRealm();
    const clientId = `client-${uuid()}`;
    const { id } = await adminClient.createClient({
      clientId,
      realm: realmName,
    });
    // "_" is a single-character wildcard in SQL LIKE, so "read-only" must not show up for "read_only".
    await adminClient.createClientRole(id, {
      name: "read_only",
      realm: realmName,
    });
    await adminClient.createClientRole(id, {
      name: "read-only",
      realm: realmName,
    });
    // Names with spaces are recorded unencoded.
    await adminClient.createClientRole(id, {
      name: "team lead",
      realm: realmName,
    });
    const readOnly = await adminClient.getClientRole(
      id,
      "read_only",
      realmName,
    );
    const teamLead = await adminClient.getClientRole(
      id,
      "team lead",
      realmName,
    );
    await adminClient.updateRealmRoleById(
      readOnly!.id!,
      { ...readOnly, description: "updated by id" },
      realmName,
    );

    await openRealm(page, realmName);
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/clients/${id}/roles/${readOnly!.id}/events`,
    );
    await page.reload();
    await assertEvents(page, ["CLIENT_ROLE CREATE", "CLIENT_ROLE UPDATE"]);

    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/clients/${id}/roles/${teamLead!.id}/events`,
    );
    await page.reload();
    await assertEvents(page, ["CLIENT_ROLE CREATE"]);
  });

  test("Client role with a wildcard in its name lists each event once", async ({
    page,
  }) => {
    const realmName = await createRealm();
    const clientId = `client-${uuid()}`;
    const { id } = await adminClient.createClient({
      clientId,
      realm: realmName,
    });
    await adminClient.createClientRole(id, { name: "read*", realm: realmName });
    await adminClient.createClientRole(id, { name: "child", realm: realmName });
    const parent = await adminClient.getClientRole(id, "read*", realmName);
    const child = await adminClient.getClientRole(id, "child", realmName);
    // Recorded below the role, so it matches both the exact and the sub-resource pattern.
    await adminClient.addCompositeRole(parent!.id!, child!, realmName);

    await openRealm(page, realmName);
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/clients/${id}/roles/${parent!.id}/events`,
    );
    await page.reload();
    await assertEvents(page, ["CLIENT_ROLE CREATE", "CLIENT_ROLE CREATE"]);
  });

  test("Client scope shows events of assigning it to a client", async ({
    page,
  }) => {
    const realmName = await createRealm();
    const name = `scope-${uuid()}`;
    await adminClient.createClientScope({
      name,
      protocol: "openid-connect",
      realm: realmName,
    });
    const clientId = `client-${uuid()}`;
    await adminClient.createClient({ clientId, realm: realmName });
    await adminClient.addDefaultClientScopeInClient(name, clientId, realmName);

    await openRealm(page, realmName);
    await goToClientScopes(page);
    await searchItem(page, "Search for client scope", name);
    await clickTableRowItem(page, name);
    await page.getByTestId("admin-events-tab").click();

    await assertEvents(page, [
      "CLIENT_SCOPE CREATE",
      "CLIENT_SCOPE_CLIENT_MAPPING CREATE",
    ]);
  });

  test("Client role does not show events of a role whose name extends its path", async ({
    page,
  }) => {
    const realmName = await createRealm();
    const clientId = `client-${uuid()}`;
    const { id } = await adminClient.createClient({
      clientId,
      realm: realmName,
    });
    await adminClient.createClientRole(id, { name: "admin", realm: realmName });
    // The create event of this role is recorded below the path of "admin".
    await adminClient.createClientRole(id, {
      name: "admin/read",
      realm: realmName,
    });
    const admin = await adminClient.getClientRole(id, "admin", realmName);

    await openRealm(page, realmName);
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/clients/${id}/roles/${admin!.id}/events`,
    );
    await page.reload();
    await assertEvents(page, ["CLIENT_ROLE CREATE"]);
  });

  test("Identity provider shows only events of its own alias", async ({
    page,
  }) => {
    const realmName = await createRealm();
    const alias = `idp-${uuid()}`;
    await adminClient.createIdentityProvider(
      "OpenID Connect v1.0",
      alias,
      realmName,
      oidcConfig,
    );
    // An alias that starts with the same text must not show up in the events of the first one.
    await adminClient.createIdentityProvider(
      "OpenID Connect v1.0",
      `${alias}-other`,
      realmName,
      oidcConfig,
    );
    await adminClient.createIdentityProviderMapper(
      alias,
      {
        name: "mapper",
        identityProviderAlias: alias,
        identityProviderMapper: "hardcoded-attribute-idp-mapper",
        config: { attribute: "a", "attribute.value": "b" },
      },
      realmName,
    );

    await openRealm(page, realmName);
    await goToIdentityProviders(page);
    await page.locator(`a[href$="/${alias}/settings"]`).click();
    await page.getByTestId("admin-events-tab").click();

    await assertEvents(page, [
      "IDENTITY_PROVIDER CREATE",
      "IDENTITY_PROVIDER_MAPPER CREATE",
    ]);
  });

  test("Identity provider treats special characters of its alias literally", async ({
    page,
  }) => {
    const realmName = await createRealm();
    // "_" is a single-character wildcard in SQL LIKE, so "corp-sso" must not show up for "corp_sso".
    for (const alias of ["corp_sso", "corp-sso"]) {
      await adminClient.createIdentityProvider(
        "OpenID Connect v1.0",
        alias,
        realmName,
        oidcConfig,
      );
      await adminClient.createIdentityProviderMapper(
        alias,
        {
          name: "mapper",
          identityProviderAlias: alias,
          identityProviderMapper: "hardcoded-attribute-idp-mapper",
          config: { attribute: "a", "attribute.value": "b" },
        },
        realmName,
      );
    }

    await openRealm(page, realmName);
    await goToIdentityProviders(page);
    await page.locator('a[href$="/corp_sso/settings"]').click();
    await page.getByTestId("admin-events-tab").click();

    await assertEvents(page, [
      "IDENTITY_PROVIDER CREATE",
      "IDENTITY_PROVIDER_MAPPER CREATE",
    ]);
  });

  test("Group shows its own and its role mapping events", async ({ page }) => {
    const realmName = await createRealm();
    const roleName = `role-${uuid()}`;
    const groupName = `group-${uuid()}`;
    await adminClient.createRealmRole({ name: roleName, realm: realmName });
    const { id } = await adminClient.createGroup(groupName, realmName);
    await adminClient.addRealmRoleToGroup(id, roleName, realmName);

    await openRealm(page, realmName);
    await goToGroups(page);
    await clickTableRowItem(page, groupName);
    await page.getByTestId("admin-events-tab").click();

    await assertEvents(page, ["GROUP CREATE", "REALM_ROLE_MAPPING CREATE"]);
  });

  test("Group shows its membership and child group events on separate tabs", async ({
    page,
  }) => {
    const realmName = await createRealm();
    const parentName = `parent-${uuid()}`;
    const [parent] = await adminClient.createSubGroups(
      [parentName, `child-${uuid()}`],
      realmName,
    );
    const { id: userId } = await adminClient.createUser({
      username: `user-${uuid()}`,
      enabled: true,
      realm: realmName,
    });
    await adminClient.addUserToGroup(userId!, parent.id, realmName);

    await openRealm(page, realmName);
    await goToGroups(page);
    await clickTableRowItem(page, parentName);
    await page.getByTestId("admin-events-tab").click();

    await page.getByRole("tab", { name: "Membership events" }).click();
    await assertEvents(page, ["GROUP_MEMBERSHIP CREATE"]);

    await page.getByRole("tab", { name: "Child group events" }).click();
    await assertEvents(page, ["GROUP CREATE"]);
  });

  async function passwordGrant(
    realmName: string,
    clientId: string,
    username: string,
  ) {
    await fetch(
      `${SERVER_URL}/realms/${realmName}/protocol/openid-connect/token`,
      {
        method: "POST",
        body: new URLSearchParams({
          grant_type: "password",
          client_id: clientId,
          username,
          password: "password",
        }),
      },
    );
  }

  async function createLoginUser(realmName: string) {
    const username = `user-${uuid()}`;
    const { id } = await adminClient.createUser({
      username,
      enabled: true,
      email: `${username}@example.org`,
      firstName: "Test",
      lastName: "User",
      credentials: [{ type: "password", value: "password", temporary: false }],
      realm: realmName,
    });
    return { id: id!, username };
  }

  async function assertLoginEvents(page: Page, count: number) {
    await expect(
      page.getByRole("row").filter({ hasText: /LOGIN/ }),
    ).toHaveCount(count);
  }

  test("User shows only its own user events", async ({ page }) => {
    const realmName = await createRealm(true);
    const user = await createLoginUser(realmName);
    const other = await createLoginUser(realmName);
    await passwordGrant(realmName, "admin-cli", user.username);
    await passwordGrant(realmName, "admin-cli", other.username);

    await openRealm(page, realmName);
    await goToUsers(page);
    await searchItem(page, "Search user", user.username);
    await clickTableRowItem(page, user.username);
    await page.getByTestId("events-tab").click();

    await assertLoginEvents(page, 1);
  });

  test("Client shows only its own user events", async ({ page }) => {
    const realmName = await createRealm(true);
    const clientId = `client-${uuid()}`;
    await adminClient.createClient({
      clientId,
      publicClient: true,
      directAccessGrantsEnabled: true,
      realm: realmName,
    });
    const user = await createLoginUser(realmName);
    await passwordGrant(realmName, clientId, user.username);
    await passwordGrant(realmName, "admin-cli", user.username);

    await openRealm(page, realmName);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await page.getByTestId("events-tab").click();

    await assertLoginEvents(page, 1);
  });

  test("User shows its own events", async ({ page }) => {
    const realmName = await createRealm();
    const username = `user-${uuid()}`;
    await adminClient.createUser({ username, enabled: true, realm: realmName });

    await openRealm(page, realmName);
    await goToUsers(page);
    await searchItem(page, "Search user", username);
    await clickTableRowItem(page, username);
    await page.getByTestId("events-tab").click();
    await page.getByRole("tab", { name: "Admin events" }).click();

    await assertEvents(page, ["USER CREATE"]);
  });

  test.describe("Organization", () => {
    async function createOrganization(realmName: string) {
      const orgName = `org-${uuid()}`;
      const { id } = await adminClient.createOrganization({
        name: orgName,
        alias: orgName,
        domains: [{ name: `${orgName}.org` }],
        realm: realmName,
      });
      return { id, orgName };
    }

    async function openOrganization(
      page: Page,
      realmName: string,
      orgName: string,
    ) {
      await openRealm(page, realmName);
      await goToOrganizations(page);
      await searchItem(page, "Search for organization", orgName);
      await clickTableRowItem(page, orgName);
    }

    test("shows its own events", async ({ page }) => {
      const realmName = await createRealm();
      const { orgName } = await createOrganization(realmName);

      await openOrganization(page, realmName, orgName);
      await page.getByTestId("admin-events-tab").click();

      await assertEvents(page, ["ORGANIZATION CREATE"]);
    });

    test("shows its members events", async ({ page }) => {
      const realmName = await createRealm();
      const { orgName } = await createOrganization(realmName);
      const { id: userId } = await adminClient.createUser({
        username: `user-${uuid()}`,
        enabled: true,
        realm: realmName,
      });
      await adminClient.addOrgMember(orgName, userId!, realmName);

      await openOrganization(page, realmName, orgName);
      await page.getByTestId("admin-events-tab").click();
      await page.getByRole("tab", { name: "Membership events" }).click();

      await assertEvents(page, ["ORGANIZATION_MEMBERSHIP CREATE"]);
    });

    test("group shows its own events", async ({ page }) => {
      const realmName = await createRealm();
      const { orgName } = await createOrganization(realmName);
      const groupName = `group-${uuid()}`;
      const { id } = await adminClient.createOrgGroup(
        orgName,
        groupName,
        realmName,
      );
      // The create event of an organization group is recorded without the group id, so only the update is listed.
      await adminClient.updateOrgGroup(
        orgName,
        id,
        { name: groupName, attributes: { a: ["b"] } },
        realmName,
      );

      await openOrganization(page, realmName, orgName);
      await page.getByTestId("groupsTab").click();
      await clickTableRowItem(page, groupName);
      // The group's tabs show up once the group is opened.
      await expect(page.getByTestId("attributesTab")).toBeVisible();
      await page.getByTestId("admin-events-tab").last().click();

      await assertEvents(page, ["GROUP UPDATE"]);
    });

    test("group shows its membership and child group events on separate tabs", async ({
      page,
    }) => {
      const realmName = await createRealm();
      const { orgName } = await createOrganization(realmName);
      const groupName = `group-${uuid()}`;
      const { id } = await adminClient.createOrgGroup(
        orgName,
        groupName,
        realmName,
      );
      await adminClient.createOrgChildGroup(
        orgName,
        id,
        `child-${uuid()}`,
        realmName,
      );
      const { id: userId } = await adminClient.createUser({
        username: `user-${uuid()}`,
        enabled: true,
        realm: realmName,
      });
      await adminClient.addOrgMember(orgName, userId!, realmName);
      await adminClient.addOrgGroupMember(orgName, id, userId!, realmName);

      await openOrganization(page, realmName, orgName);
      await page.getByTestId("groupsTab").click();
      await clickTableRowItem(page, groupName);
      await expect(page.getByTestId("attributesTab")).toBeVisible();
      await page.getByTestId("admin-events-tab").last().click();

      await page.getByRole("tab", { name: "Membership events" }).last().click();
      await assertEvents(page, ["ORGANIZATION_GROUP_MEMBERSHIP CREATE"]);

      await page
        .getByRole("tab", { name: "Child group events" })
        .last()
        .click();
      await assertEvents(page, ["GROUP CREATE"]);
    });
  });
});
