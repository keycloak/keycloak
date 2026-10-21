import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertRequiredFieldError } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import { clickTableRowItem, searchItem } from "../utils/table.ts";
import { continueNext, createClient, save } from "./utils.ts";
import {
  assertDedicatedScopeMapper,
  assertKeyForCodeExchangeInput,
  goToDedicatedScope,
  selectKeyForCodeExchangeInput,
  toggleFrontchannelLogout,
  toggleFullScopeAllowed,
} from "./details.ts";

test.describe.serial("Clients details test", () => {
  const realmName = `clients-details-realm-${uuid()}`;
  const clientId = `client-details-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createClient({
      clientId,
      protocol: "openid-connect",
      publicClient: false,
      realm: realmName,
    });
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
    await adminClient.deleteClient(clientId);
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
  });

  test("Should test clientId required", async ({ page }) => {
    await createClient(page);
    await assertRequiredFieldError(page, "clientId");
  });

  test("Cancel create should return to clients", async ({ page }) => {
    await createClient(
      page,
      { clientId },
      async () => await page.getByRole("button", { name: "Cancel" }).click(),
    );

    await expect(page).not.toHaveURL("add-client");
  });

  test("Should be able to create a client", async ({ page }) => {
    await createClient(page, {
      clientId: `created-client-${uuid()}`,
      name: "ClientName",
      description: "ClientDescription",
    });

    await continueNext(page);
    await save(page);

    await assertNotificationMessage(page, "Client created successfully");
  });

  test("Should be able to update a client", async ({ page }) => {
    await clickTableRowItem(page, clientId);
    await selectKeyForCodeExchangeInput(page, "S256");
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");
    await assertKeyForCodeExchangeInput(page, "S256");
  });
});

test.describe.serial("Client admin without manage-realm", () => {
  const realmName = `clients-delegated-admin-realm-${uuid()}`;
  const clientId = `client-delegated-admin-${uuid()}`;
  const username = `client-admin-${uuid()}`;
  const password = "password";
  const mapperName = "hardcoded-claim";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { enabled: true });
    await adminClient.createClient({
      clientId,
      protocol: "openid-connect",
      publicClient: false,
      realm: realmName,
      protocolMappers: [
        {
          name: mapperName,
          protocol: "openid-connect",
          protocolMapper: "oidc-hardcoded-claim-mapper",
          config: {
            "claim.name": "test-claim",
            "claim.value": "test-value",
            "jsonType.label": "String",
            "access.token.claim": "true",
          },
        },
      ],
    });

    const { id } = await adminClient.createUser({
      realm: realmName,
      username,
      enabled: true,
      emailVerified: true,
      credentials: [{ type: "password", temporary: false, value: password }],
      firstName: "Client",
      lastName: "Admin",
      email: "client-admin@keycloak.org",
    });
    // view-clients is needed to open the client in the admin console,
    // manage-clients to update it. manage-realm is intentionally missing.
    await adminClient.addClientRoleToUser(
      id!,
      "realm-management",
      ["view-clients", "manage-clients"],
      realmName,
    );
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test("Should save client settings and full scope without touching protocol mappers", async ({
    page,
  }) => {
    await login(page, { realm: realmName, username, password });
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);

    // Save client settings
    const initialFrontchannelLogout = (
      await adminClient.getClient(clientId, realmName)
    )?.frontchannelLogout;
    await toggleFrontchannelLogout(page);
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");

    // Protocol mapper is still there in the dedicated scope of the client
    await goToDedicatedScope(page, clientId);
    await assertDedicatedScopeMapper(page, mapperName);

    // Update "Full scope allowed" and verify nothing else has changed
    await toggleFullScopeAllowed(page);
    await assertNotificationMessage(page, "Scope mapping updated");
    await assertDedicatedScopeMapper(page, mapperName);

    const client = await adminClient.getClient(clientId, realmName);
    expect(client?.fullScopeAllowed).toBe(false);
    expect(client?.frontchannelLogout).toBe(!initialFrontchannelLogout);
    expect(client?.protocolMappers).toHaveLength(1);
    expect(client?.protocolMappers?.[0].name).toBe(mapperName);
  });
});
