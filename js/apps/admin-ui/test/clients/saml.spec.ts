import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { switchOff, switchOn } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { assertModalTitle, cancelModal, confirmModal } from "../utils/modal.ts";
import { goToClients } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { goToAdvancedTab, revertFineGrain, saveFineGrain } from "./advanced.ts";
import {
  assertCertificates,
  assertEncryptionAlgorithm,
  assertEncryptionKeyAlgorithm,
  assertEncryptionDigestMethod,
  assertEncryptionMaskGenerationFunction,
  assertEncryptionAlgorithmInputVisible,
  assertEncryptionKeyAlgorithmInputVisible,
  assertEncryptionDigestMethodInputVisible,
  assertEncryptionMaskGenerationFunctionInputVisible,
  assertNameIdFormatDropdown,
  assertSamlClientDetails,
  assertTermsOfServiceUrl,
  clickClientSignature,
  clickEncryptionAssertions,
  clickOffEncryptionAssertions,
  clickGenerate,
  clickPostBinding,
  goToClientSettingsTab,
  goToKeysTab,
  saveSamlSettings,
  selectEncryptionAlgorithmInput,
  selectEncryptionKeyAlgorithmInput,
  selectEncryptionDigestMethodInput,
  selectEncryptionMaskGenerationFunctionInput,
  setTermsOfServiceUrl,
} from "./saml.ts";

test.describe.serial("Fine Grain SAML Endpoint Configuration", () => {
  const clientName = `saml-advanced-tab-${uuid()}`;

  test.beforeAll(() =>
    adminClient.createClient({
      protocol: "saml",
      clientId: clientName,
      publicClient: false,
    }),
  );

  test.afterAll(() => adminClient.deleteClient(clientName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToClients(page);
    await clickTableRowItem(page, clientName);
    await goToAdvancedTab(page);
  });

  test("should set Terms of service URL", async ({ page }) => {
    const termsOfServiceUrl = "http://some.url/terms-of-service.html";

    // Set and save URL
    await setTermsOfServiceUrl(page, termsOfServiceUrl);
    await saveFineGrain(page);
    await assertNotificationMessage(page, "Client successfully updated");

    // Try to set different URL but revert
    await setTermsOfServiceUrl(page, "http://not.saveing.this/");
    await revertFineGrain(page);

    // Verify original URL remains
    await assertTermsOfServiceUrl(page, termsOfServiceUrl);
  });

  test("should show error for invalid terms of service URL", async ({
    page,
  }) => {
    await setTermsOfServiceUrl(page, "not a url");
    await saveFineGrain(page);
    await assertNotificationMessage(
      page,
      "Client could not be updated: invalid_inputTerms of service URL is not a valid URL",
    );
  });
});

test.describe.serial("Clients SAML tests", () => {
  const clientId = "saml";

  const clientName = `saml-settings-${uuid()}`;

  test.beforeAll(() =>
    adminClient.createClient({
      protocol: "saml",
      clientId: clientName,
    }),
  );

  test.afterAll(() => adminClient.deleteClient(clientName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToClients(page);
    await clickTableRowItem(page, clientId);
  });

  test("should display the saml sections on details screen", async ({
    page,
  }) => {
    await assertSamlClientDetails(page);
  });

  test("should save force name id format", async ({ page }) => {
    await clickPostBinding(page);
    await saveSamlSettings(page);
    await assertNotificationMessage(page, "Client successfully updated");
  });

  test("should not disable signature when cancel", async ({ page }) => {
    await goToKeysTab(page);
    await clickClientSignature(page);
    await assertModalTitle(page, 'Disable "Client signature required"');
    await cancelModal(page);
    await assertCertificates(page);
  });

  test("should disable client signature", async ({ page }) => {
    await goToKeysTab(page);
    await clickClientSignature(page);
    await assertModalTitle(page, 'Disable "Client signature required"');
    await confirmModal(page);
    await assertNotificationMessage(page, "Client successfully updated");
    await assertCertificates(page);
  });

  test("should enable Encryption keys config", async ({ page }) => {
    // enable encryption on keys tab
    await goToKeysTab(page);
    await clickEncryptionAssertions(page);
    await clickGenerate(page);
    await assertNotificationMessage(
      page,
      "New key pair and certificate generated successfully",
    );
    await confirmModal(page);
    await assertCertificates(page);

    // assert encryption algorithms can be modified
    await goToClientSettingsTab(page);
    await assertEncryptionAlgorithm(page, "Choose...");
    await assertEncryptionKeyAlgorithm(page, "Choose...");
    await assertEncryptionDigestMethodInputVisible(page, false);
    await assertEncryptionMaskGenerationFunctionInputVisible(page, false);
    await selectEncryptionAlgorithmInput(page, "AES_256_GCM");
    await selectEncryptionKeyAlgorithmInput(page, "RSA-OAEP-11");
    await assertEncryptionDigestMethod(page, "Choose...");
    await assertEncryptionMaskGenerationFunction(page, "Choose...");
    await selectEncryptionDigestMethodInput(page, "SHA-256");
    await selectEncryptionMaskGenerationFunctionInput(page, "mgf1sha256");
    await selectEncryptionKeyAlgorithmInput(page, "RSA1_5");
    await assertEncryptionDigestMethodInputVisible(page, false);
    await assertEncryptionMaskGenerationFunctionInputVisible(page, false);
    await selectEncryptionKeyAlgorithmInput(page, "RSA-OAEP-11");
    await assertEncryptionDigestMethod(page, "Choose...");
    await assertEncryptionMaskGenerationFunction(page, "Choose...");
    await selectEncryptionDigestMethodInput(page, "SHA-256");
    await selectEncryptionMaskGenerationFunctionInput(page, "mgf1sha256");

    // disable encryption and check encryption algorithms are hidden
    await goToKeysTab(page);
    await clickOffEncryptionAssertions(page);
    await confirmModal(page);
    await goToClientSettingsTab(page);
    await assertEncryptionAlgorithmInputVisible(page, false);
    await assertEncryptionKeyAlgorithmInputVisible(page, false);
    await assertEncryptionDigestMethodInputVisible(page, false);
    await assertEncryptionMaskGenerationFunctionInputVisible(page, false);

    // enable encryption again and check algorithms are empty/default
    await goToKeysTab(page);
    await clickEncryptionAssertions(page);
    await confirmModal(page);
    await goToClientSettingsTab(page);
    await assertEncryptionAlgorithm(page, "Choose...");
    await assertEncryptionKeyAlgorithm(page, "Choose...");
    await assertEncryptionDigestMethodInputVisible(page, false);
    await assertEncryptionMaskGenerationFunctionInputVisible(page, false);
  });

  test("should check SAML capabilities", async ({ page }) => {
    // Assert SAML Capabilities switches exist
    const switches = [
      ['[data-testid="attributes.saml_force_name_id_format"]', "on"],
      ['[data-testid="attributes.saml.artifact.binding"]', "on"],
      ['[data-testid="attributes.saml.artifact.binding"]', "off"],
      ['[data-testid="attributes.saml.server.signature"]', "off"],
      ['[data-testid="attributes.saml.assertion.signature"]', "on"],
    ];

    for (const [name, value] of switches) {
      if (value === "off") {
        await switchOff(page, name);
      } else {
        await switchOn(page, name);
      }
    }

    // Assert Name ID Format dropdown exists
    await assertNameIdFormatDropdown(page);
  });

  test("should check access settings", async ({ page }) => {
    const validUrl =
      "http://localhost:8180/realms/master/protocol/" + clientId + "/clients/";
    const invalidUrlErrorRoot =
      "Client could not be updated: invalid_inputRoot URL is not a valid URL";
    const invalidUrlErrorBase =
      "Client could not be updated: invalid_inputBase URL is not a valid URL";

    await page.getByTestId("rootUrl").fill("Invalid URL");
    await saveSamlSettings(page);
    await assertNotificationMessage(page, invalidUrlErrorRoot);
    await page.getByTestId("rootUrl").clear();

    await page.getByTestId("baseUrl").fill("Invalid URL");
    await saveSamlSettings(page);

    await assertNotificationMessage(page, invalidUrlErrorBase);
    await page.getByTestId("baseUrl").clear();

    await page.getByTestId("rootUrl").fill(validUrl);
    await page.getByTestId("baseUrl").fill(validUrl);
    await saveSamlSettings(page);

    await assertNotificationMessage(page, "Client successfully updated");
  });
});
