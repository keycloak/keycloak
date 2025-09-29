import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { assertRequiredFieldError } from "../utils/form.ts";
import { chooseFile } from "../utils/file-chooser.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { assertModalTitle, confirmModal } from "../utils/modal.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import {
  clearAllFilters,
  clickRowKebabItem,
  getRowByCellText,
  searchItem,
} from "../utils/table.ts";
import {
  cancel,
  clientCapabilityConfig,
  continueNext,
  createClient,
  save,
} from "./utils.ts";

test.describe.serial("Clients test", () => {
  const realmName = `clients-realm-${uuid()}`;
  const clientId = `clientId`;
  const placeHolder = "Search for client";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
  });

  test("Should test clientId required", async ({ page }) => {
    await createClient(page);
    await assertRequiredFieldError(page, "clientId");
  });

  test("Cancel create should return to clients", async ({ page }) => {
    await createClient(page, { clientId }, () => cancel(page));

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

  test("Should fail creating client that already exists", async ({ page }) => {
    await createClient(page, {
      clientId: "account",
      name: "${account}",
    });

    await continueNext(page);
    await save(page);

    await assertNotificationMessage(
      page,
      "Could not create client: 'Client account already exists'",
    );
  });

  test("Create client", async ({ page }) => {
    const client = {
      clientId,
      name: "ClientName",
      description: "ClientDescription",
      alwaysDisplayInConsole: true,
      publicClient: true,
      directAccessGrantsEnabled: true,
      implicitFlowEnabled: true,
      serviceAccountsEnabled: true,
      standardFlowEnabled: true,
      attributes: {
        "oauth2.device.authorization.grant.enabled": "true",
        "oidc.ciba.grant.enabled": "true",
      },
    };
    await createClient(page, client);
    await clientCapabilityConfig(page, client);
    await continueNext(page);
    await save(page);

    await assertNotificationMessage(page, "Client created successfully");

    await goToClients(page);
    await clickRowKebabItem(page, clientId, "Delete");
    await assertModalTitle(page, `Delete ${clientId} ?`);
    await confirmModal(page);
    await assertNotificationMessage(page, "The client has been deleted");
  });

  test("Search for clients", async ({ page }) => {
    await searchItem(page, placeHolder, "John Doe");
    await expect(
      page.getByRole("heading", { name: "No search results" }),
    ).toBeVisible();

    await clearAllFilters(page);
    await expect(getRowByCellText(page, "account")).toBeVisible();
  });

  test.describe.serial("Clients import", () => {
    test.beforeAll(() =>
      adminClient.createClient({
        clientId: "identical",
        protocol: "openid-connect",
        realm: realmName,
      }),
    );

    test.afterAll(() => adminClient.deleteClient("identical"));

    test("Import client", async ({ page }) => {
      await page.getByTestId("importClient").click();
      await chooseFile(page, "../utils/files/import-identical-client.json");
      await save(page);
      await assertNotificationMessage(
        page,
        "Could not import client: Client identical already exists",
      );
    });
  });
});
