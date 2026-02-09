import { expect, test } from "@playwright/test";
import {
  createJwtAuthorizationGrantProvider,
  createJwtAuthorizationGrantProviderKey,
  clickSaveButton,
} from "./main.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { login } from "../utils/login.ts";
import adminClient from "../utils/AdminClient.ts";
import { assertModalTitle, confirmModal } from "../utils/modal.ts";
import { selectItem } from "../utils/form.ts";
import { chooseFileByLocator } from "../utils/file-chooser.ts";

test.describe.serial("JWT Authorization Grant identity provider test", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToIdentityProviders(page);
  });

  test.afterEach(() =>
    adminClient.deleteIdentityProvider("jwt-authorization-grant"),
  );

  test("should create a JWT Authorization Grant provider with JWKS url", async ({
    page,
  }) => {
    await createJwtAuthorizationGrantProvider(
      page,
      "jwt-authorization-grant",
      "https://localhost/realms/test",
      "https://localhost/realms/test/protocol/openid-connect/certs",
    );

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );

    await goToIdentityProviders(page);
    await clickTableRowItem(page, "jwt-authorization");

    await expect(page.getByTestId("config.issuer")).toHaveValue(
      "https://localhost/realms/test",
    );
    await expect(page.getByTestId("config.useJwksUrl")).toBeChecked();
    await expect(page.getByTestId("config.jwksUrl")).toHaveValue(
      "https://localhost/realms/test/protocol/openid-connect/certs",
    );

    await page
      .getByTestId("config.issuer")
      .fill("https://localhost/realms/test2");
    await page
      .getByTestId("config.jwksUrl")
      .fill("https://localhost/realms/test2/protocol/openid-connect/certs");

    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");

    await expect(page.getByTestId("config.issuer")).toHaveValue(
      "https://localhost/realms/test2",
    );
    await expect(page.getByTestId("config.jwksUrl")).toHaveValue(
      "https://localhost/realms/test2/protocol/openid-connect/certs",
    );
  });

  test("should create a JWT Authorization Grant provider with public key pem", async ({
    page,
  }) => {
    await createJwtAuthorizationGrantProviderKey(
      page,
      "jwt-authorization-grant",
      "https://localhost/realms/test",
      "keyId",
      "MEMwBQYDK2VxAzoAWOVoLNsZlgw5dvat/Xi83Rh7zQMOerq3XrTT1xVbqDX2naZPlza0gwyNnMV6H6vnUGbaCK/+mgCA",
    );

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );

    await goToIdentityProviders(page);
    await clickTableRowItem(page, "jwt-authorization-grant");

    await expect(page.getByTestId("config.issuer")).toHaveValue(
      "https://localhost/realms/test",
    );
    await expect(page.getByTestId("config.useJwksUrl")).not.toBeChecked();
    await expect(page.getByTestId("config.jwksUrl")).toBeHidden();
    await expect(
      page.getByTestId("config.publicKeySignatureVerifierKeyId"),
    ).toHaveValue("keyId");
    await expect(
      page.getByTestId("config.publicKeySignatureVerifier"),
    ).toHaveValue(
      "MEMwBQYDK2VxAzoAWOVoLNsZlgw5dvat/Xi83Rh7zQMOerq3XrTT1xVbqDX2naZPlza0gwyNnMV6H6vnUGbaCK/+mgCA",
    );

    await page.getByTestId("import-certificate-button").click();
    await assertModalTitle(page, "Import key");
    await selectItem(page, page.locator("#keystoreFormat"), "Public Key PEM");
    await chooseFileByLocator(
      page,
      "../utils/files/key.pem",
      page.locator("#importFile-browse-button"),
    );
    await confirmModal(page);

    await expect(
      page.getByTestId("config.publicKeySignatureVerifier"),
    ).toHaveValue(/MIIBI/);

    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");

    await page.getByTestId("import-certificate-button").click();
    await assertModalTitle(page, "Import key");
    await selectItem(page, page.locator("#keystoreFormat"), "JSON Web Key Set");
    await chooseFileByLocator(
      page,
      "../utils/files/key.jwks",
      page.locator("#importFile-browse-button"),
    );
    await confirmModal(page);

    await expect(
      page.getByTestId("config.publicKeySignatureVerifier"),
    ).toHaveValue(/{ "keys" : /);

    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");
  });
});
