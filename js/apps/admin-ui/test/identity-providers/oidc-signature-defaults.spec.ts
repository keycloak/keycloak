import { type Page, expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { clickSwitch } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";

const realmName = `idp-oidc-defaults-${uuid()}`;
const addOidcProviderUrl = `${SERVER_URL}/admin/master/console/#/${realmName}/identity-providers/oidc/add`;
const oidcProviderName = "OpenID Connect v1.0";

test.beforeAll(() => adminClient.createRealm(realmName));
test.afterAll(() => adminClient.deleteRealm(realmName));

test.describe("OIDC signature defaults", () => {
  const alias = `oidc-sig-defaults-${uuid()}`;

  async function disableDiscovery(page: Parameters<typeof login>[0]) {
    await page.locator('label[for="kc-discovery-endpoint-switch"]').click();
    await expect(
      page.locator("#kc-discovery-endpoint-switch"),
    ).not.toBeChecked();
  }

  test.afterAll(async () => {
    if (await adminClient.getIdentityProvider(alias, realmName)) {
      await adminClient.deleteIdentityProvider(alias, realmName);
    }
  });

  test("should enable the validate-signature switch by default without discovery", async ({
    page,
  }) => {
    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
    await page.goto(addOidcProviderUrl);
    await disableDiscovery(page);

    await expect(page.getByTestId("config.validateSignature")).toBeChecked();
  });

  test("should persist validateSignature=true when creating a provider without discovery", async ({
    page,
  }) => {
    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
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

    const provider = await adminClient.getIdentityProvider(alias, realmName);
    expect(provider?.config?.validateSignature).toBe("true");
  });
});

test.describe("OIDC provider with missing signature config", () => {
  const alias = `oidc-missing-sig-config-${uuid()}`;

  test.afterAll(async () => {
    if (await adminClient.getIdentityProvider(alias, realmName)) {
      await adminClient.deleteIdentityProvider(alias, realmName);
    }
  });

  test("should show key controls when signature field is absent in backend", async ({
    page,
  }) => {
    await adminClient.createIdentityProvider(
      oidcProviderName,
      alias,
      realmName,
      {
        useJwksUrl: "true",
        jwksUrl: "https://idp.example/jwks",
      },
    );

    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
    await goToIdentityProviders(page);
    await clickTableRowItem(page, oidcProviderName);

    const validateSignature = page.getByTestId("config.validateSignature");
    await expect(validateSignature).toBeChecked();

    const useJwksUrl = page.getByTestId("config.useJwksUrl");
    await expect(useJwksUrl).toBeVisible();

    const jwksUrl = page.getByTestId("config.jwksUrl");
    await expect(jwksUrl).toBeVisible();

    // The switch default must not leave the form dirty on load.
    await expect(page.getByTestId("idp-details-save")).toBeDisabled();
  });
});

test.describe("OIDC provider with explicit signature config", () => {
  const createProvider = async (alias: string, validateSignature: string) => {
    await adminClient.createIdentityProvider(
      oidcProviderName,
      alias,
      realmName,
      {
        validateSignature,
        useJwksUrl: "true",
        authorizationUrl: "https://idp.example/auth",
        tokenUrl: "https://idp.example/token",
        clientId: "broker-client",
        clientSecret: "broker-secret",
        jwksUrl: "https://idp.example/jwks",
      },
    );
  };

  const openProvider = async (page: Page) => {
    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
    await goToIdentityProviders(page);
    await clickTableRowItem(page, oidcProviderName);
    await page.getByTestId("config.validateSignature").waitFor({
      state: "attached",
    });
  };

  const cleanUp = async (alias: string) => {
    if (await adminClient.getIdentityProvider(alias, realmName)) {
      await adminClient.deleteIdentityProvider(alias, realmName);
    }
  };

  test.describe("validateSignature=false", () => {
    const alias = `oidc-explicit-sig-${uuid()}`;

    test.afterAll(() => cleanUp(alias));

    test("should hide the JWKS controls", async ({ page }) => {
      await createProvider(alias, "false");
      await openProvider(page);

      await expect(
        page.getByTestId("config.validateSignature"),
      ).not.toBeChecked();

      await expect(page.getByTestId("config.useJwksUrl")).toBeHidden();
      await expect(page.getByTestId("config.jwksUrl")).toBeHidden();
    });
  });

  test.describe("validateSignature=true", () => {
    const alias = `oidc-explicit-sig-${uuid()}`;

    test.afterAll(() => cleanUp(alias));

    test("should show the JWKS controls", async ({ page }) => {
      await createProvider(alias, "true");
      await openProvider(page);

      await expect(page.getByTestId("config.validateSignature")).toBeChecked();

      await expect(page.getByTestId("config.useJwksUrl")).toBeVisible();
      await expect(page.getByTestId("config.jwksUrl")).toBeVisible();
    });
  });
});

for (const providerName of [oidcProviderName, "Keycloak OpenID Connect"]) {
  test(`should preserve a static key for ${providerName} when the JWKS toggle is absent`, async ({
    page,
  }) => {
    const alias = `oidc-static-key-${uuid()}`;
    const staticKey =
      "MEMwBQYDK2VxAzoAWOVoLNsZlgw5dvat/Xi83Rh7zQMOerq3XrTT1xVbqDX2naZPlza0gwyNnMV6H6vnUGbaCK/+mgCA";
    const displayName = "Updated OIDC provider";

    await adminClient.createIdentityProvider(providerName, alias, realmName, {
      authorizationUrl: "https://idp.example/auth",
      tokenUrl: "https://idp.example/token",
      clientId: "broker-client",
      clientSecret: "broker-secret",
      publicKeySignatureVerifier: staticKey,
    });
    const provider = await adminClient.getIdentityProvider(alias, realmName);
    expect(provider?.config?.useJwksUrl).toBeUndefined();

    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
    await goToIdentityProviders(page);
    await clickTableRowItem(page, providerName);

    await expect(page.getByTestId("config.useJwksUrl")).not.toBeChecked();
    await expect(page.getByTestId("config.jwksUrl")).toBeHidden();
    await expect(
      page.getByTestId("config.publicKeySignatureVerifier"),
    ).toHaveValue(staticKey);
    const save = page.getByTestId("idp-details-save");
    await expect(save).toBeDisabled();
    await page.getByTestId("displayName").fill(displayName);
    await save.click();
    await assertNotificationMessage(page, "Provider successfully updated");

    const updated = await adminClient.getIdentityProvider(alias, realmName);
    expect(updated?.displayName).toBe(displayName);
    expect(updated?.config?.publicKeySignatureVerifier).toBe(staticKey);
    expect(updated?.config?.useJwksUrl).toBe("false");

    const useJwksUrl = page.getByTestId("config.useJwksUrl");
    const jwksUrl = page.getByTestId("config.jwksUrl");
    await clickSwitch(page, useJwksUrl);
    await jwksUrl.fill("https://idp.example/jwks");
    await save.click();
    await assertNotificationMessage(page, "Provider successfully updated");

    const jwksProvider = await adminClient.getIdentityProvider(
      alias,
      realmName,
    );
    expect(jwksProvider?.config?.publicKeySignatureVerifier).toBe(staticKey);
    await page.reload();
    await expect(useJwksUrl).toBeChecked();
    await expect(jwksUrl).toHaveValue("https://idp.example/jwks");
  });
}
