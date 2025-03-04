import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { assertRequiredFieldError } from "../utils/form";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { goToClients, goToRealm } from "../utils/sidebar";
import { searchItem } from "../utils/table";
import { continueNext, createClient, save } from "./utils";

test.describe("Clients details test", () => {
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
});
