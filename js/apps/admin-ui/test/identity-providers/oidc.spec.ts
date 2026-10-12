import { test, expect } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { switchOn } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { SERVER_URL } from "../utils/constants.ts";
import {
  addMapper,
  assertAuthorizationUrl,
  assertInvalidUrlNotification,
  assertJwksUrlExists,
  assertOnMappingPage,
  assertPkceMethodExists,
  clickCancelMapper,
  clickRevertButton,
  clickSaveButton,
  clickSaveMapper,
  createOIDCProvider,
  goToMappersTab,
  reenterClientSecret,
  setUrl,
} from "./main.ts";

test.describe.serial("OIDC identity provider test", () => {
  const oidcProviderName = "oidc";
  const secret = "123";

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToIdentityProviders(page);
  });

  test.afterAll(() => adminClient.deleteIdentityProvider(oidcProviderName));

  test("should create an OIDC provider using discovery url", async ({
    page,
  }) => {
    await createOIDCProvider(page, oidcProviderName, secret);
    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );

    const provider = await adminClient.getIdentityProvider(oidcProviderName);
    expect(provider?.config?.validateSignature).toBe("true");

    await assertAuthorizationUrl(page);

    await setUrl(page, "authorization", "invalid");
    await clickSaveButton(page);
    await assertInvalidUrlNotification(page, "authorization");
    await clickRevertButton(page);
    await assertAuthorizationUrl(page);

    // Changing token destination fields clears the masked client secret; re-enter it to save.
    await setUrl(page, "token", "invalid");
    await reenterClientSecret(page, secret);
    await clickSaveButton(page);
    await assertInvalidUrlNotification(page, "token");
    await clickRevertButton(page);

    await setUrl(page, "tokenIntrospection", "invalid");
    await reenterClientSecret(page, secret);
    await clickSaveButton(page);
    await assertInvalidUrlNotification(page, "tokenIntrospection");
    await clickRevertButton(page);

    await assertJwksUrlExists(page);
    await page.getByText("Use JWKS URL").click();
    await assertJwksUrlExists(page, false);

    await assertPkceMethodExists(page, false);
    await switchOn(page, "#config\\.pkceEnabled");
    await assertPkceMethodExists(page);

    await clickSaveButton(page);
    await expect(page.getByText("Required field")).toBeVisible();

    await switchOn(page, "#config\\.useJwksUrl");
    await assertJwksUrlExists(page, true);
    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");
  });
});

test.describe.serial("Edit OIDC Provider", () => {
  const oidcProviderName = "OpenID Connect v1.0";
  const alias = `edit-oidc-${uuid()}`;

  test.beforeEach(async ({ page }) => {
    await adminClient.createIdentityProvider(
      oidcProviderName,
      alias,
      undefined,
      { validateSignature: "false" },
    );
    await login(page);
    await goToIdentityProviders(page);
    await clickTableRowItem(page, oidcProviderName);
  });

  test.afterEach(() => adminClient.deleteIdentityProvider(alias));

  test("should add OIDC mapper of type Attribute Importer", async ({
    page,
  }) => {
    const mapperName = "OIDC Attribute Importer";
    await goToMappersTab(page);
    await addMapper(page, "oidc-user-attribute", mapperName);
    await clickSaveMapper(page);
    await expect(
      page.getByRole("row", {
        name: new RegExp(mapperName, "i"),
      }),
    ).toBeVisible();
  });

  test("should add OIDC mapper of type Claim To Role", async ({ page }) => {
    const mapperName = "OIDC Claim to Role";
    await goToMappersTab(page);
    await addMapper(page, "oidc-role", mapperName);
    await clickSaveMapper(page);
    await expect(
      page.getByRole("row", {
        name: new RegExp(mapperName, "i"),
      }),
    ).toBeVisible();
  });

  test("should cancel the addition of the OIDC mapper", async ({ page }) => {
    await goToMappersTab(page);
    await addMapper(page, "oidc-role", "OIDC Claim to Role");
    await clickCancelMapper(page);
    await assertOnMappingPage(page);
  });
});

test.describe("OIDC signature defaults", () => {
  const alias = `oidc-sig-defaults-${uuid()}`;
  const addOidcUrl = `${SERVER_URL}/admin/master/console/#/master/identity-providers/oidc/add`;

  async function disableDiscovery(page: Parameters<typeof login>[0]) {
    await page.locator("#kc-discovery-endpoint-switch").click();
  }

  test.afterAll(() => adminClient.deleteIdentityProvider(alias));

  test("should enable the validate-signature switch by default on the add form", async ({
    page,
  }) => {
    await login(page);
    await page.goto(addOidcUrl);
    await disableDiscovery(page);

    const validateSignature = page.getByTestId("config.validateSignature");
    await validateSignature.waitFor({ state: "attached" });
    await expect(validateSignature).toBeChecked();
  });

  test("should persist validateSignature=true when creating a provider without discovery", async ({
    page,
  }) => {
    await login(page);
    await page.goto(addOidcUrl);
    await disableDiscovery(page);

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
