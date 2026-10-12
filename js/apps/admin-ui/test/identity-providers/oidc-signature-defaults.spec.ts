import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";

const addOidcProviderUrl = `${SERVER_URL}/admin/master/console/#/master/identity-providers/oidc/add`;

test.describe("OIDC signature defaults", () => {
  const alias = `oidc-sig-defaults-${uuid()}`;

  async function disableDiscovery(page: Parameters<typeof login>[0]) {
    await page.locator('label[for="kc-discovery-endpoint-switch"]').click();
    await expect(
      page.locator("#kc-discovery-endpoint-switch"),
    ).not.toBeChecked();
  }

  test.afterAll(async () => {
    if (await adminClient.getIdentityProvider(alias)) {
      await adminClient.deleteIdentityProvider(alias);
    }
  });

  test("should enable the validate-signature switch by default without discovery", async ({
    page,
  }) => {
    await login(page);
    await page.goto(addOidcProviderUrl);
    await disableDiscovery(page);

    await expect(page.getByTestId("config.validateSignature")).toBeChecked();
  });

  test("should persist validateSignature=true when creating a provider without discovery", async ({
    page,
  }) => {
    await login(page);
    await page.goto(addOidcProviderUrl);
    await disableDiscovery(page);

    const validateSignature = page.getByTestId("config.validateSignature");
    await expect(validateSignature).toBeChecked();

    await page.getByTestId("alias").fill(alias);
    await page
      .getByTestId("config.authorizationUrl")
      .fill("https://idp.example/auth");
    await page.getByTestId("config.tokenUrl").fill("https://idp.example/token");
    await page.getByTestId("config.clientId").fill("broker-client");
    await page.getByTestId("config.clientSecret").fill("broker-secret");
    await page.getByTestId("config.jwksUrl").fill("https://idp.example/jwks");
    await page.getByTestId("createProvider").click();

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );

    const provider = await adminClient.getIdentityProvider(alias);
    expect(provider?.config?.validateSignature).toBe("true");
  });
});
