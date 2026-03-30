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
  assertAccessTokenSigningKeyValue,
  assertAllSigningKeyDropdownsVisible,
  assertIdTokenSigningKeyValue,
  assertUserInfoSigningKeyValue,
  assertAuthorizationResponseSigningKeyValue,
  selectKeyForCodeExchangeInput,
  selectAccessTokenSigningKey,
  selectIdTokenSigningKey,
  selectUserInfoSigningKey,
  selectAuthorizationResponseSigningKey,
  assertAccessTokenSigningKeyDisplayText,
  toggleLogoutConfirmation,
} from "./details.ts";
import { goToAdvancedTab, saveFineGrain } from "./advanced.ts";

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

test.describe.serial("OIDC Signing Key Selection", () => {
  const realmName = `oidc-signing-key-${uuid()}`;
  const clientId = `oidc-signing-test-${uuid()}`;
  const testKeyName = "test-rsa-key";
  let testKeyKid: string;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    // Add an ACTIVE key with lower priority than the default (100)
    await adminClient.addKeyProvider(
      testKeyName,
      true,
      true,
      "rsa-generated",
      realmName,
      50,
    );
    const keys = await adminClient.getRealmKeys(realmName);
    const testKey = keys.find(
      (k) =>
        k.status === "ACTIVE" &&
        k.providerPriority === 50 &&
        (k as { use?: string }).use === "SIG",
    );
    testKeyKid = testKey?.kid || "";
    await adminClient.createClient({
      realm: realmName,
      clientId,
      protocol: "openid-connect",
      publicClient: false,
    });
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
    await clickTableRowItem(page, clientId);
  });

  test("should display all signing key dropdowns in advanced tab", async ({
    page,
  }) => {
    await goToAdvancedTab(page);
    await assertAllSigningKeyDropdownsVisible(page);
    await assertAccessTokenSigningKeyValue(page, "");
    await assertIdTokenSigningKeyValue(page, "");
    await assertUserInfoSigningKeyValue(page, "");
    await assertAuthorizationResponseSigningKeyValue(page, "");
  });

  test("should select and save signing keys for all token types", async ({
    page,
  }) => {
    await goToAdvancedTab(page);
    await selectAccessTokenSigningKey(page, testKeyKid);
    await selectIdTokenSigningKey(page, testKeyKid);
    await selectUserInfoSigningKey(page, testKeyKid);
    await selectAuthorizationResponseSigningKey(page, testKeyKid);
    await saveFineGrain(page);
    await assertNotificationMessage(page, "Client successfully updated");
    await page.reload();
    await goToAdvancedTab(page);
    await assertAccessTokenSigningKeyValue(page, testKeyKid);
    await assertIdTokenSigningKeyValue(page, testKeyKid);
    await assertUserInfoSigningKeyValue(page, testKeyKid);
    await assertAuthorizationResponseSigningKeyValue(page, testKeyKid);
  });

  test("should show key status changes: passive, disabled, not found", async ({
    page,
  }) => {
    // Configure access token signing key
    await goToAdvancedTab(page);
    await selectAccessTokenSigningKey(page, testKeyKid);
    await saveFineGrain(page);
    await assertNotificationMessage(page, "Client successfully updated");

    // Make key passive and verify display
    await adminClient.makeKeyProviderPassive(testKeyName, realmName);
    await page.reload();
    await goToAdvancedTab(page);
    await assertAccessTokenSigningKeyDisplayText(
      page,
      new RegExp(`\\(Passive\\).*${testKeyKid}`),
    );

    // Disable key and verify display
    await adminClient.disableKeyProvider(testKeyName, realmName);
    await page.reload();
    await goToAdvancedTab(page);
    await assertAccessTokenSigningKeyDisplayText(
      page,
      new RegExp(`\\(Disabled\\).*${testKeyKid}`),
    );

    // Delete key and verify not found display
    await adminClient.deleteKeyProvider(testKeyName, realmName);
    await page.reload();
    await goToAdvancedTab(page);
    await assertAccessTokenSigningKeyDisplayText(
      page,
      new RegExp(`Not found.*${testKeyKid}`),
    );
  });
});
