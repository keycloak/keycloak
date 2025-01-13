import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../../cypress/support/util/AdminClient";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { goToClients } from "../utils/sidebar";
import { clickTableRowItem } from "../utils/table";
import { goToAdvancedTab, revertFineGrain, saveFineGrain } from "./advanced";
import { assertTermsOfServiceUrl, setTermsOfServiceUrl } from "./saml";

test.describe("Fine Grain SAML Endpoint Configuration", () => {
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
