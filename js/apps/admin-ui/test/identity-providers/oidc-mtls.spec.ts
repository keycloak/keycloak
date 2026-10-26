import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { clickSaveButton } from "./main.ts";

test.describe("OIDC identity provider with mutual TLS client authentication", () => {
  const providerName = "OpenID Connect v1.0";
  const alias = `mtls-oidc-${uuid()}`;
  const mtlsMethod = "Client certificate with mutual TLS";
  const field = "#config\\.clientCertificateKeyProviderId";

  test.beforeEach(async ({ page }) => {
    await adminClient.createIdentityProvider(providerName, alias, undefined, {
      clientId: "mtls-client",
      clientSecret: "secret",
      clientAuthMethod: "client_secret_post",
      authorizationUrl: "https://idp.example.org/auth",
      tokenUrl: "https://localhost:1/token",
    });
    await login(page);
    await goToIdentityProviders(page);
    await clickTableRowItem(page, providerName);
  });

  test.afterEach(() => adminClient.deleteIdentityProvider(alias));

  test("should replace the client secret with the client certificates of the realm", async ({
    page,
  }) => {
    await expect(page.getByTestId("config.clientSecret")).toBeVisible();
    await expect(page.getByTestId("config.mtlsTokenUrl")).toBeHidden();

    await selectItem(page, "#config\\.clientAuthMethod", mtlsMethod);
    await expect(page.getByTestId("config.clientSecret")).toBeHidden();
    await expect(page.getByTestId("config.mtlsTokenUrl")).toBeVisible();
    await expect(page.getByTestId("config.mtlsUserInfoUrl")).toBeVisible();
    await expect(
      page.getByTestId("config.mtlsTokenIntrospectionUrl"),
    ).toBeVisible();

    // only the key providers with a certificate can be used as the client certificate
    await page.locator(field).click();
    const option = page.getByRole("option", { name: /^rsa-generated/ });
    await expect(option).toBeVisible();
    // the subject and the expiry of the certificate are shown with the name
    await expect(option).toContainText("Subject: ");
    await expect(option).toContainText("CN=");
    await expect(option).toContainText("Expires: ");
    await expect(
      page.getByRole("option", { name: /^(hmac|aes)-generated/ }),
    ).toHaveCount(0);
  });

  test("should save the key provider selected as the client certificate", async ({
    page,
  }) => {
    await selectItem(page, "#config\\.clientAuthMethod", mtlsMethod);
    await selectItem(
      page,
      field,
      page.getByRole("option", { name: /^rsa-generated/ }),
    );
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");

    await page.reload();
    await expect(page.locator("#config\\.clientAuthMethod")).toContainText(
      mtlsMethod,
    );
    await expect(page.locator(field)).toContainText("rsa-generated");
    await expect(page.getByTestId("config.clientSecret")).toBeHidden();
  });

  test("should show the details of the selected client certificate", async ({
    page,
  }) => {
    await selectItem(page, "#config\\.clientAuthMethod", mtlsMethod);
    await expect(page.getByTestId("viewClientCertificateDetails")).toBeHidden();
    await selectItem(
      page,
      field,
      page.getByRole("option", { name: /^rsa-generated/ }),
    );

    await page.getByTestId("viewClientCertificateDetails").click();
    const details = page.getByRole("dialog", { name: "Certificate details" });
    await expect(details).toContainText("SHA-256 fingerprint");
    await expect(details).toContainText("Serial number");
    await details.getByTestId("close").click();
    await expect(details).toBeHidden();
  });

  test("should show a saved key provider that was deleted", async ({
    page,
  }) => {
    const alias = `mtls-oidc-${uuid()}`;
    // the key provider has to exist to be saved, it is deleted afterwards
    const keyProviderName = `mtls-key-${uuid()}`;
    const keyProviderId = await adminClient.addKeyProvider(
      keyProviderName,
      true,
      true,
      "rsa-generated",
    );
    await adminClient.createIdentityProvider(providerName, alias, undefined, {
      clientId: "mtls-client",
      clientAuthMethod: "tls_client_auth",
      clientCertificateKeyProviderId: keyProviderId,
      authorizationUrl: "https://idp.example.org/auth",
      tokenUrl: "https://idp.example.org/token",
    });
    await adminClient.deleteKeyProvider(keyProviderId!);
    try {
      // both providers have the same display name in the list, so open this one by its alias
      await page.goto(
        `${SERVER_URL}/admin/master/console/#/master/identity-providers/oidc/${alias}/settings`,
      );
      // changing only the hash keeps the identity provider opened in beforeEach
      await page.reload();
      await expect(page.locator(field)).toContainText(
        `Unavailable: ${keyProviderId}`,
      );
    } finally {
      await adminClient.deleteIdentityProvider(alias);
    }
  });

  test("should test the selected client certificate", async ({ page }) => {
    await selectItem(page, "#config\\.clientAuthMethod", mtlsMethod);
    await expect(page.getByTestId("testClientCertificate")).toBeDisabled();
    await selectItem(
      page,
      field,
      page.getByRole("option", { name: /^rsa-generated/ }),
    );

    // nothing listens at the token url of the identity provider
    await page.getByTestId("testClientCertificate").click();
    await expect(page.getByTestId("last-alert")).toContainText(
      "Client certificate test failed",
    );
  });

  test("should show the client secret again for the other methods", async ({
    page,
  }) => {
    await selectItem(page, "#config\\.clientAuthMethod", mtlsMethod);
    await expect(page.getByTestId("config.clientSecret")).toBeHidden();

    await selectItem(
      page,
      "#config\\.clientAuthMethod",
      "Client secret sent in the request body",
    );
    await expect(page.getByTestId("config.clientSecret")).toBeVisible();
    await expect(page.getByTestId("config.mtlsTokenUrl")).toBeHidden();
    await expect(page.locator(field)).toBeHidden();
  });
});
