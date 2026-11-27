import { type Page, expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { SERVER_URL } from "../utils/constants.ts";
import {
  assertRequiredFieldError,
  switchOff,
  switchOn,
} from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { clickSaveButton } from "./main.ts";

const realmName = `idp-saml-metadata-signature-${uuid()}`;
const samlProviderName = "SAML v2.0";
const samlServiceUrl = `${SERVER_URL}/realms/${realmName}/protocol/saml`;
const samlDescriptorUrl = `${samlServiceUrl}/descriptor`;
const metadataSigningCertificate =
  "MIIBkTCB+wIJAKHBfpegPjMCMA0GCSqGSIb3DQEBCwUAMA0xCzAJBgNVBAYTAlVT";

test.beforeAll(() => adminClient.createRealm(realmName));
test.afterAll(() => adminClient.deleteRealm(realmName));

test.describe("SAML metadata signature validation settings", () => {
  const openProvider = async (page: Page) => {
    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
    await goToIdentityProviders(page);
    await clickTableRowItem(page, samlProviderName);
    await page.getByTestId("config.useMetadataDescriptorUrl").waitFor({
      state: "attached",
    });
  };

  const cleanUp = async (alias: string) => {
    if (await adminClient.getIdentityProvider(alias, realmName)) {
      await adminClient.deleteIdentityProvider(alias, realmName);
    }
  };

  test.describe("useMetadataDescriptorUrl is not true", () => {
    const alias = `saml-metadata-signature-${uuid()}`;

    test.afterAll(() => cleanUp(alias));

    test("should show the metadata signature switch only when the metadata descriptor URL is used", async ({
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
      await openProvider(page);

      const useMetadataDescriptorUrl = page.getByTestId(
        "config.useMetadataDescriptorUrl",
      );
      const validateMetadataSignature = page.getByTestId(
        "config.validateMetadataSignature",
      );

      await expect(useMetadataDescriptorUrl).not.toBeChecked();
      await expect(validateMetadataSignature).toBeHidden();
      await expect(
        page.getByTestId("config.metadataSigningCertificate"),
      ).toBeHidden();

      await switchOn(page, useMetadataDescriptorUrl);

      await expect(validateMetadataSignature).toBeAttached();
      await expect(validateMetadataSignature).not.toBeChecked();
      await expect(
        page.getByTestId("config.metadataSigningCertificate"),
      ).toBeHidden();

      await switchOff(page, useMetadataDescriptorUrl);

      await expect(validateMetadataSignature).toBeHidden();
    });
  });

  test.describe("useMetadataDescriptorUrl is true", () => {
    const alias = `saml-metadata-signature-${uuid()}`;

    test.afterAll(() => cleanUp(alias));

    test("should require and save the metadata signing certificates", async ({
      page,
    }) => {
      await adminClient.createIdentityProvider(
        samlProviderName,
        alias,
        realmName,
        {
          // The single sign-on service URL is required to save the provider
          singleSignOnServiceUrl: samlServiceUrl,
          useMetadataDescriptorUrl: "true",
          metadataDescriptorUrl: samlDescriptorUrl,
        },
      );
      await openProvider(page);

      const validateMetadataSignature = page.getByTestId(
        "config.validateMetadataSignature",
      );
      const certificate = page.getByTestId("config.metadataSigningCertificate");

      await expect(validateMetadataSignature).not.toBeChecked();
      await expect(certificate).toBeHidden();

      await switchOn(page, validateMetadataSignature);
      await expect(certificate).toBeVisible();

      // The certificates are required when the signature of the metadata is validated
      await clickSaveButton(page);
      await assertRequiredFieldError(page, "config.metadataSigningCertificate");

      await certificate.fill(metadataSigningCertificate);
      await clickSaveButton(page);
      await assertNotificationMessage(page, "Provider successfully updated");

      const saved = await adminClient.getIdentityProvider(alias, realmName);
      expect(saved?.config).toMatchObject({
        validateMetadataSignature: "true",
        metadataSigningCertificate,
      });

      await page.reload();
      await expect(validateMetadataSignature).toBeChecked();
      await expect(certificate).toHaveValue(metadataSigningCertificate);

      await switchOff(page, validateMetadataSignature);
      await expect(certificate).toBeHidden();
    });
  });
});
