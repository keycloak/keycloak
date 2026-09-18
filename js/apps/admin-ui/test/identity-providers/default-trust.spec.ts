import { expect, test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { switchOff, switchOn } from "../utils/form.ts";
import { clickSaveButton, createDefaultTrustProvider } from "./main.ts";

const alias = "default-trust";
const addDefaultTrustProviderUrl = `${SERVER_URL}/admin/master/console/#/master/identity-providers/default-trust/add`;
const jwksUrl = "https://localhost/realms/test/protocol/openid-connect/certs";
const jwks = '{"keys":[]}';

test.describe.serial("Default Trust identity provider test", () => {
  test.beforeEach(async ({ page }) => {
    try {
      await adminClient.deleteIdentityProvider(alias);
    } catch {
      // The provider may not exist before the test starts.
    }

    await login(page);
    await goToIdentityProviders(page);
  });

  test.afterEach(async ({}, testInfo) => {
    if (testInfo.title.includes("create and edit")) {
      await adminClient.deleteIdentityProvider(alias);
    }
  });

  test("should only show trust material settings", async ({ page }) => {
    await page.goto(addDefaultTrustProviderUrl);

    await expect(page.getByTestId("alias")).toHaveValue(alias);
    await expect(page.getByTestId("config.useX509")).not.toBeChecked();
    await expect(page.getByTestId("config.useJwksUrl")).toBeChecked();
    await expect(page.getByTestId("config.jwksUrl")).toBeVisible();

    await expect(page.getByTestId("displayName")).toBeHidden();
    await expect(page.getByTestId("config.clientId")).toBeHidden();
    await expect(page.getByTestId("config.clientSecret")).toBeHidden();
    await expect(page.getByTestId("displayOrder")).toBeHidden();

    await switchOff(page, page.getByTestId("config.useJwksUrl"));

    await expect(page.getByTestId("config.jwksUrl")).toBeHidden();
    await expect(
      page.getByTestId("config.publicKeySignatureVerifier"),
    ).toBeVisible();
    await expect(
      page.getByTestId("config.publicKeySignatureVerifierKeyId"),
    ).toBeVisible();
    await expect(page.getByTestId("import-certificate-button")).toBeVisible();

    await switchOn(page, page.getByTestId("config.useX509"));
    await expect(page.getByTestId("config.useJwksUrl")).toBeHidden();
    await expect(page.getByTestId("config.trustedCertificates")).toBeVisible();
    await expect(
      page.getByTestId("config.requiredExtendedKeyUsages"),
    ).toBeVisible();
  });

  test("should create and edit a Default Trust provider", async ({ page }) => {
    await createDefaultTrustProvider(page, alias, jwksUrl);

    await goToIdentityProviders(page);
    await clickTableRowItem(page, alias);

    await expect(page.getByTestId("config.useJwksUrl")).toBeChecked();
    await expect(page.getByTestId("config.jwksUrl")).toHaveValue(jwksUrl);
    await expect(page.getByTestId("displayName")).toBeHidden();
    await expect(page.getByTestId("config.clientId")).toBeHidden();
    await expect(page.getByTestId("config.clientSecret")).toBeHidden();
    await expect(page.getByTestId("mappers-tab")).toBeHidden();

    await switchOn(page, page.getByTestId("config.useX509"));
    await page
      .getByTestId("config.trustedCertificates")
      .fill("stale certificate");
    await page.getByTestId("config.requiredExtendedKeyUsages").fill("1.2.3.4");
    await switchOff(page, page.getByTestId("config.useX509"));
    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");

    await switchOff(page, page.getByTestId("config.useJwksUrl"));
    await page.getByTestId("config.publicKeySignatureVerifier").fill(jwks);
    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");
    await expect(page.getByTestId("config.useJwksUrl")).not.toBeChecked();
    await expect(
      page.getByTestId("config.publicKeySignatureVerifier"),
    ).toHaveValue(jwks);
  });
});
