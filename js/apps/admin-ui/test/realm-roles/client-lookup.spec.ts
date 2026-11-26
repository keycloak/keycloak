import { expect, type Page, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { goToRolesTab } from "../clients/role.ts";
import adminClient from "../utils/AdminClient.ts";
import { clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToClients, goToRealm, goToRealmRoles } from "../utils/sidebar.ts";
import { clickTableRowItem, searchItem } from "../utils/table.ts";

test.describe.serial("Realm role client lookup", () => {
  const realmName = `realm-role-lookup-${uuid()}`;
  const roleName = `realm-role-${uuid()}`;
  const roleDescription = "some description";
  const clientName = `client-with-role-${uuid()}`;
  const clientRoleName = `client-role-${uuid()}`;
  const restrictedUser = {
    username: `role-manager-${uuid()}`,
    password: "role-manager-password",
  };
  let clientObjectId: string;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { enabled: true });
    await adminClient.createRealmRole({
      realm: realmName,
      name: roleName,
      description: roleDescription,
    });

    // The user may manage realm roles, but must not gain any client visibility.
    const { id } = await adminClient.createUser({
      realm: realmName,
      username: restrictedUser.username,
      enabled: true,
      emailVerified: true,
      credentials: [{ type: "password", value: restrictedUser.password }],
      firstName: "Role",
      lastName: "Manager",
      email: "role-manager@keycloak.org",
    });
    await adminClient.addClientRoleToUser(
      id!,
      "realm-management",
      ["view-realm", "view-users", "manage-realm"],
      realmName,
    );

    const { id: clientId } = await adminClient.createClient({
      realm: realmName,
      clientId: clientName,
      protocol: "openid-connect",
      publicClient: false,
    });
    clientObjectId = clientId!;
    await adminClient.createClientRole(clientObjectId, {
      realm: realmName,
      name: clientRoleName,
      description: roleDescription,
    });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  // The clients collection endpoint requires view-clients permission, which
  // the restricted user does not have. Rejecting it here is what the server
  // does when the console looks up clients for them; with the client lookup
  // guarded (#55225) the realm role page must never send this request.
  async function rejectClientsCollection(page: Page) {
    const requests: string[] = [];
    await page.route(/\/admin\/realms\/[^/]+\/clients\/?$/, (route) => {
      requests.push(route.request().url());
      return route.fulfill({ status: 403, json: { error: "unknown_error" } });
    });
    return requests;
  }

  test("edits a realm role without looking up the client", async ({ page }) => {
    const clientsRequests = await rejectClientsCollection(page);
    await login(page, {
      realm: realmName,
      username: restrictedUser.username,
      password: restrictedUser.password,
    });
    await goToRealmRoles(page);

    await searchItem(page, "Search role by name", roleName);
    await clickTableRowItem(page, roleName);

    // The role is rendered instead of the error boundary shown when the
    // client lookup fails with 403.
    await expect(page.getByTestId("view-header")).toContainText(roleName);
    await expect(
      page.getByText("You do not have permission to access this resource"),
    ).toHaveCount(0);
    expect(clientsRequests).toEqual([]);

    // The role can also be edited without the view-clients permission.
    const updatedDescription = "updated description";
    await page.fill("textarea[name='description']", updatedDescription);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "The role has been saved");
  });

  test("looks up the client when editing a client role", async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);

    await clickTableRowItem(page, clientName);
    await goToRolesTab(page);

    const clientLookup = page.waitForResponse((response) =>
      response.url().endsWith(`/clients/${clientObjectId}`),
    );
    await clickTableRowItem(page, clientRoleName);
    expect((await clientLookup).ok()).toBe(true);

    await expect(page.getByTestId("view-header")).toContainText(clientRoleName);
  });
});
