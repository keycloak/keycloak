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
  assertKeyForCodeExchangeInput,
  enableJwtAuthorizationGrant,
  getJwtAuthorizationGrantIdpOptions,
  selectKeyForCodeExchangeInput,
  toggleLogoutConfirmation,
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
    await toggleLogoutConfirmation(page);
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");
    await assertKeyForCodeExchangeInput(page, "S256");
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
