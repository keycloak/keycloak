import { type Page, expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";

const realmName = `idp-saml-defaults-${uuid()}`;
const addSamlProviderUrl = `${SERVER_URL}/admin/master/console/#/${realmName}/identity-providers/saml/add`;
const samlDescriptorUrl = `${SERVER_URL}/realms/${realmName}/protocol/saml/descriptor`;

test.beforeAll(() => adminClient.createRealm(realmName));
test.afterAll(() => adminClient.deleteRealm(realmName));

test("should enable SAML signature switches by default", async ({ page }) => {
  await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
  await page.goto(addSamlProviderUrl);

  const wantAssertionsSigned = page.getByTestId("config.wantAssertionsSigned");
  await wantAssertionsSigned.waitFor({ state: "attached" });
  await expect(wantAssertionsSigned).toBeChecked();

  const validateSignature = page.getByTestId("config.validateSignature");
  await validateSignature.waitFor({ state: "attached" });
  await expect(validateSignature).toBeChecked();
});

test.describe("SAML provider with missing signature config", () => {
  const samlProviderName = "SAML v2.0";
  const alias = `saml-missing-sig-config-${uuid()}`;

  test.afterAll(async () => {
    if (await adminClient.getIdentityProvider(alias, realmName)) {
      await adminClient.deleteIdentityProvider(alias, realmName);
    }
  });

  test("should show key controls when signature fields are absent in backend", async ({
    page,
  }) => {
    await adminClient.createIdentityProvider(
      samlProviderName,
      alias,
      realmName,
    );

    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
    await goToIdentityProviders(page);
    await clickTableRowItem(page, samlProviderName);

    const wantAssertionsSigned = page.getByTestId(
      "config.wantAssertionsSigned",
    );
    await expect(wantAssertionsSigned).toBeChecked();

    const validateSignature = page.getByTestId("config.validateSignature");
    await expect(validateSignature).toBeChecked();

    const signingCertificate = page.getByTestId("config.signingCertificate");
    await expect(signingCertificate).toBeVisible();

    const metadataDescriptorUrl = page.getByTestId(
      "config.metadataDescriptorUrl",
    );
    await expect(metadataDescriptorUrl).toBeVisible();
  });
});

test.describe("SAML key management actions visibility", () => {
  const samlProviderName = "SAML v2.0";

  test.describe("Reload action", () => {
    const alias = `saml-reload-keys-${uuid()}`;

    test.afterAll(async () => {
      if (await adminClient.getIdentityProvider(alias, realmName)) {
        await adminClient.deleteIdentityProvider(alias, realmName);
      }
    });

    test("should show Reload keys action when useMetadataDescriptorUrl is true", async ({
      page,
    }) => {
      await adminClient.createIdentityProvider(
        samlProviderName,
        alias,
        realmName,
        {
          useMetadataDescriptorUrl: "true",
          metadataDescriptorUrl: samlDescriptorUrl,
        },
      );

      await login(page, {
        to: { pathname: `/${realmName}/identity-providers` },
      });
      await goToIdentityProviders(page);
      await clickTableRowItem(page, samlProviderName);

      await page.getByTestId("action-dropdown").click();

      await expect(page.getByText("Reload keys")).toBeVisible();

      await expect(page.getByText("Import keys")).toBeHidden();
    });
  });

  test.describe("Import action", () => {
    const alias = `saml-import-keys-${uuid()}`;

    test.afterAll(async () => {
      if (await adminClient.getIdentityProvider(alias, realmName)) {
        await adminClient.deleteIdentityProvider(alias, realmName);
      }
    });

    test("should show Import keys action when useMetadataDescriptorUrl is not true", async ({
      page,
    }) => {
      await adminClient.createIdentityProvider(
        samlProviderName,
        alias,
        realmName,
        {
          metadataDescriptorUrl: samlDescriptorUrl,
        },
      );

      await login(page, {
        to: { pathname: `/${realmName}/identity-providers` },
      });
      await goToIdentityProviders(page);
      await clickTableRowItem(page, samlProviderName);

      await page.getByTestId("action-dropdown").click();

      await expect(page.getByText("Import keys")).toBeVisible();

      await expect(page.getByText("Reload keys")).toBeHidden();
    });
  });
});

test.describe("SAML provider with explicit signature config", () => {
  const samlProviderName = "SAML v2.0";

  const createProvider = async (
    alias: string,
    validateSignature: string,
    wantAssertionsSigned: string,
  ) => {
    await adminClient.createIdentityProvider(
      samlProviderName,
      alias,
      realmName,
      {
        validateSignature,
        wantAssertionsSigned,
        metadataDescriptorUrl: samlDescriptorUrl,
      },
    );
  };

  const openProvider = async (page: Page) => {
    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
    await goToIdentityProviders(page);
    await clickTableRowItem(page, samlProviderName);
    await page.getByTestId("config.validateSignature").waitFor({
      state: "attached",
    });
  };

  const cleanUp = async (alias: string) => {
    if (await adminClient.getIdentityProvider(alias, realmName)) {
      await adminClient.deleteIdentityProvider(alias, realmName);
    }
  };

  test.describe("validateSignature=false and wantAssertionsSigned=true", () => {
    const alias = `saml-explicit-sig-${uuid()}`;

    test.afterAll(() => cleanUp(alias));

    test("should show the signing key controls", async ({ page }) => {
      await createProvider(alias, "false", "true");
      await openProvider(page);

      await expect(
        page.getByTestId("config.validateSignature"),
      ).not.toBeChecked();
      await expect(
        page.getByTestId("config.wantAssertionsSigned"),
      ).toBeChecked();

      // Assertion verification needs keys even when the response is not validated
      await expect(page.getByTestId("config.signingCertificate")).toBeVisible();
      await expect(
        page.getByTestId("config.metadataDescriptorUrl"),
      ).toBeVisible();
      await page.getByTestId("action-dropdown").click();
      await expect(page.getByText("Import keys")).toBeVisible();
    });
  });

  test.describe("validateSignature=true and wantAssertionsSigned=false", () => {
    const alias = `saml-explicit-sig-${uuid()}`;

    test.afterAll(() => cleanUp(alias));

    test("should show the signing key controls", async ({ page }) => {
      await createProvider(alias, "true", "false");
      await openProvider(page);

      await expect(page.getByTestId("config.validateSignature")).toBeChecked();
      await expect(
        page.getByTestId("config.wantAssertionsSigned"),
      ).not.toBeChecked();

      await expect(page.getByTestId("config.signingCertificate")).toBeVisible();
      await expect(
        page.getByTestId("config.metadataDescriptorUrl"),
      ).toBeVisible();
      await page.getByTestId("action-dropdown").click();
      await expect(page.getByText("Import keys")).toBeVisible();
    });
  });

  test.describe("validateSignature=false and wantAssertionsSigned=false", () => {
    const alias = `saml-explicit-sig-${uuid()}`;

    test.afterAll(() => cleanUp(alias));

    test("should hide the signing key controls", async ({ page }) => {
      await createProvider(alias, "false", "false");
      await openProvider(page);

      await expect(
        page.getByTestId("config.validateSignature"),
      ).not.toBeChecked();
      await expect(
        page.getByTestId("config.wantAssertionsSigned"),
      ).not.toBeChecked();

      await expect(page.getByTestId("config.signingCertificate")).toBeHidden();
      await expect(
        page.getByTestId("config.metadataDescriptorUrl"),
      ).toBeHidden();
      await page.getByTestId("action-dropdown").click();
      await expect(page.getByText("Import keys")).toBeHidden();
    });
  });
});
