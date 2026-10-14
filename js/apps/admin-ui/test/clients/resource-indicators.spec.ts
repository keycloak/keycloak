import { expect, test, type Page } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { toClient } from "../../src/clients/routes/Client.tsx";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { save } from "./utils.ts";

// The integration server is always started with the `resource-indicators`
// feature (locally via js/apps/keycloak-server/scripts/start-server.js and in
// CI via the `--features` list in .github/workflows/js-ci.yml, #47121), so
// these tests assert the switch renders and fail loudly if it is absent,
// rather than skipping.
test.describe.serial("Client resource indicators", () => {
  const realmName = `resource-indicators-realm-${uuid()}`;
  const clientId = `resource-indicators-client-${uuid()}`;

  let clientUuid: string;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);

    const client = await adminClient.createClient({
      realm: realmName,
      clientId,
      protocol: "openid-connect",
      publicClient: false,
    });
    clientUuid = client.id!;
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  async function goToClientSettings(page: Page) {
    await login(page, {
      to: toClient({
        realm: realmName,
        clientId: clientUuid,
        tab: "settings",
      }),
    });

    await expect(page.getByTestId("resource-indicators-enabled")).toBeVisible();
  }

  test("is disabled by default", async ({ page }) => {
    await goToClientSettings(page);

    await expect(
      page.getByTestId("resource-indicators-enabled"),
    ).not.toBeChecked();
  });

  test("can be enabled and persists after reload", async ({ page }) => {
    await goToClientSettings(page);

    await page.getByTestId("resource-indicators-enabled").check();
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");

    await page.reload();
    await expect(page.getByTestId("resource-indicators-enabled")).toBeChecked();

    const client = await adminClient.getClient(clientId, realmName);
    expect(client?.attributes?.["resource.indicators.enabled"]).toBe("true");
  });

  test("can be disabled again", async ({ page }) => {
    await goToClientSettings(page);
    await expect(page.getByTestId("resource-indicators-enabled")).toBeChecked();

    await page.getByTestId("resource-indicators-enabled").uncheck();
    await save(page);
    await assertNotificationMessage(page, "Client successfully updated");

    const client = await adminClient.getClient(clientId, realmName);
    expect(client?.attributes?.["resource.indicators.enabled"]).toBe("false");
  });
});
