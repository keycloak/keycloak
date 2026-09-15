import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { switchOff } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { goToClients, goToRealm } from "../utils/sidebar.ts";
import { clickTableRowItem, searchItem } from "../utils/table.ts";

test.describe.serial("Clients OIDC keys", () => {
  const realmName = `clients-keys-realm-${uuid()}`;
  const clientId = `client-keys-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createClient({
      clientId,
      protocol: "openid-connect",
      publicClient: false,
      realm: realmName,
      attributes: {
        "use.jwks.url": "true",
      },
    });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToClients(page);
    await searchItem(page, "Search for client", clientId);
    await clickTableRowItem(page, clientId);
    await page.getByTestId("keysTab").click();
  });

  test("Should enable reload when JWKS URL is disabled", async ({ page }) => {
    await expect(page.getByTestId("reload")).toBeVisible();
    await expect(page.getByTestId("reload")).toBeDisabled();

    await switchOff(page, page.getByTestId("attributes.use.jwks.url"));

    await expect(page.getByTestId("reload")).toBeEnabled();
  });
});
