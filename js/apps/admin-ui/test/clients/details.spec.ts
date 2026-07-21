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
  assertJwtAlgorithmOptions,
  assertKeyForCodeExchangeInput,
  assertMacAlgorithmLabel,
  assertSignatureAlgorithmLabel,
  goToCredentialsTab,
  selectClientAuthenticator,
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

  test("Should use MAC terminology for client secret JWT algorithms", async ({
    page,
  }) => {
    await clickTableRowItem(page, clientId);
    await goToCredentialsTab(page);

    await selectClientAuthenticator(page, "Signed JWT with Client Secret");
    await assertMacAlgorithmLabel(page);
    await assertJwtAlgorithmOptions(
      page,
      ["HS256", "HS384", "HS512"],
      ["RS256", "ES256"],
    );

    await selectClientAuthenticator(page, "Signed JWT");
    await assertSignatureAlgorithmLabel(page);
    await assertJwtAlgorithmOptions(page, ["RS256", "ES256"], ["HS256"]);
  });
});
