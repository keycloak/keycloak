import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";

const addSamlProviderUrl = `${SERVER_URL}/admin/master/console/#/master/identity-providers/saml/add`;

test("should enable SAML signature switches by default", async ({ page }) => {
  await login(page);
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
    if (await adminClient.getIdentityProvider(alias)) {
      await adminClient.deleteIdentityProvider(alias);
    }
  });

  test("should show key controls when signature fields are absent in backend", async ({
    page,
  }) => {
    await adminClient.createIdentityProvider(samlProviderName, alias);

    await login(page);
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
  const samlDescriptorUrl = `${SERVER_URL}/realms/master/protocol/saml/descriptor`;

  test.describe("Reload action", () => {
    const alias = `saml-reload-keys-${uuid()}`;

    test.afterAll(async () => {
      if (await adminClient.getIdentityProvider(alias)) {
        await adminClient.deleteIdentityProvider(alias);
      }
    });

    test("should show Reload keys action when useMetadataDescriptorUrl is true", async ({
      page,
    }) => {
      await adminClient.createIdentityProvider(
        samlProviderName,
        alias,
        undefined,
        {
          useMetadataDescriptorUrl: "true",
          metadataDescriptorUrl: samlDescriptorUrl,
        },
      );

      await login(page);
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
      if (await adminClient.getIdentityProvider(alias)) {
        await adminClient.deleteIdentityProvider(alias);
      }
    });

    test("should show Import keys action when useMetadataDescriptorUrl is not true", async ({
      page,
    }) => {
      await adminClient.createIdentityProvider(
        samlProviderName,
        alias,
        undefined,
        {
          metadataDescriptorUrl: samlDescriptorUrl,
        },
      );

      await login(page);
      await goToIdentityProviders(page);
      await clickTableRowItem(page, samlProviderName);

      await page.getByTestId("action-dropdown").click();

      await expect(page.getByText("Import keys")).toBeVisible();

      await expect(page.getByText("Reload keys")).toBeHidden();
    });
  });
});
