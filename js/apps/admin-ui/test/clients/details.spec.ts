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
  enableJwtAuthorizationGrant,
  getJwtAuthorizationGrantIdpOptions,
  goToDedicatedScope,
  selectKeyForCodeExchangeInput,
  toggleFullScopeAllowed,
  toggleLogoutConfirmation,
  setResourceUrl,
  assertResourceUrl,
  setValidRedirectUri,
  clickAddValidRedirectUri,
  clickRemoveValidRedirectUri,
  setValidPostLogoutRedirectUri,
  clickAddValidPostLogoutRedirectUri,
  clickRemoveValidPostLogoutRedirectUri,
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
    await toggleLogoutConfirmation(page);
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");
    await assertKeyForCodeExchangeInput(page, "S256");
  });

  test("Should be able to set the resource URL", async ({ page }) => {
    const resourceUrl = "https://resource.example.com";
    await clickTableRowItem(page, clientId);
    await setResourceUrl(page, resourceUrl);
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");
    await page.reload();
    await assertResourceUrl(page, resourceUrl);
  });

  test("Should be able to assign valid redirect URIs", async ({ page }) => {
    const validRedirectUri1 = "https://localhost:8081/app";
    const validRedirectUri2 = "https://127.0.0.1:8081/app";
    await clickTableRowItem(page, clientId);

    await setValidRedirectUri(page, 0, validRedirectUri1);
    await clickAddValidRedirectUri(page);
    await setValidRedirectUri(page, 1, validRedirectUri2);
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");
    const client1 = await adminClient.getClient(clientId, realmName);
    expect(client1?.redirectUris).toEqual([
      validRedirectUri1,
      validRedirectUri2,
    ]);

    await clickRemoveValidRedirectUri(page, 1);
    await setValidRedirectUri(page, 0, "");
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");
    const client2 = await adminClient.getClient(clientId, realmName);
    expect(client2?.redirectUris).toEqual([]);
  });

  test("Should be able to assign valid post logout redirect URIs", async ({
    page,
  }) => {
    const validRedirectUri1 = "https://localhost:8081/app";
    const validRedirectUri2 = "https://127.0.0.1:8081/app";
    await clickTableRowItem(page, clientId);

    await setValidPostLogoutRedirectUri(page, 0, validRedirectUri1);
    await clickAddValidPostLogoutRedirectUri(page);
    await setValidPostLogoutRedirectUri(page, 1, validRedirectUri2);
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");
    const client1 = await adminClient.getClient(clientId, realmName);
    expect(client1?.attributes?.["post.logout.redirect.uris"]).toEqual(
      `${validRedirectUri1}##${validRedirectUri2}`,
    );

    await clickRemoveValidPostLogoutRedirectUri(page, 1);
    await setValidPostLogoutRedirectUri(page, 0, "");
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");
    const client2 = await adminClient.getClient(clientId, realmName);
    expect(client2?.attributes?.["post.logout.redirect.uris"]).toBeUndefined();
  });
});

test.describe.serial("Client admin without manage-realm", () => {
  const realmName = `clients-delegated-admin-realm-${uuid()}`;
  const clientId = `client-delegated-admin-${uuid()}`;
  const username = `client-admin-${uuid()}`;
  const password = "password";
  const mapperName = "hardcoded-claim";
  const resourceUrl = "https://delegated-admin.example.com";

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
    await setResourceUrl(page, resourceUrl);
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");

    await page.reload();
    await assertResourceUrl(page, resourceUrl);

    // Protocol mapper is still there in the dedicated scope of the client
    await goToDedicatedScope(page, clientId);
    await assertDedicatedScopeMapper(page, mapperName);

    // Update "Full scope allowed" and verify nothing else has changed
    await toggleFullScopeAllowed(page);
    await assertNotificationMessage(page, "Scope mapping updated");
    await assertDedicatedScopeMapper(page, mapperName);

    const client = await adminClient.getClient(clientId, realmName);
    expect(client?.fullScopeAllowed).toBe(false);
    expect(client?.attributes?.resource_url).toBe(resourceUrl);
    expect(client?.protocolMappers).toHaveLength(1);
    expect(client?.protocolMappers?.[0].name).toBe(mapperName);
  });
});

test.describe
  .serial("JWT authorization grant identity provider selection", () => {
  const realmName = `clients-jwt-grant-realm-${uuid()}`;
  const clientId = `client-jwt-grant-${uuid()}`;
  const orgName = "jwt-grant-org";
  const realmIdpAlias = "realm-jwt-grant-idp";
  const orgIdpAlias = "org-jwt-grant-idp";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createClient({
      clientId,
      protocol: "openid-connect",
      publicClient: false,
      realm: realmName,
    });

    // Two providers of the same type, one plain realm provider and one that
    // gets linked to an organization, so the assertion below distinguishes
    // "the list is populated" from "the list is not filtered by realmOnly".
    // An issuer is required and must be unique per provider type, so it is
    // derived from the alias.
    for (const alias of [realmIdpAlias, orgIdpAlias]) {
      await adminClient.createIdentityProvider(
        "JWT Authorization Grant",
        alias,
        realmName,
        { issuer: `https://${alias}.example.com` },
      );
    }

    await adminClient.createOrganization({
      realm: realmName,
      name: orgName,
      domains: [{ name: "jwt-grant-org.com", verified: false }],
    });
    await adminClient.linkIdpToOrganization(orgName, orgIdpAlias, realmName);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test("Should keep organization-linked providers selectable", async ({
    page,
  }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);

    await enableJwtAuthorizationGrant(page);

    const options = await getJwtAuthorizationGrantIdpOptions(page);
    expect(options).toContain(realmIdpAlias);
    expect(options).toContain(orgIdpAlias);
  });
});
