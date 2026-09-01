import { test } from "@playwright/test";
import {
  createJwtAuthorizationGrantProvider,
  createJwtAuthorizationGrantProviderKey,
  clickSaveButton,
} from "./main.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { login } from "../utils/login.ts";
import adminClient from "../utils/AdminClient.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import {
  assertJwtProviderWithJwksUrl,
  assertJwtProviderWithPublicKey,
  assertJwtPublicKeySignatureVerifier,
  fillJwtProviderWithJwksUrl,
  importJwtJwksFile,
  importJwtPublicKeyPemFile,
  openJwtAuthorizationGrantProvider,
} from "./jwt-authorization-grant.ts";

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

    await openJwtAuthorizationGrantProvider(page, "jwt-authorization");
    await assertJwtProviderWithJwksUrl(
      page,
      "https://localhost/realms/test",
      "https://localhost/realms/test/protocol/openid-connect/certs",
    );

    await fillJwtProviderWithJwksUrl(
      page,
      "https://localhost/realms/test2",
      "https://localhost/realms/test2/protocol/openid-connect/certs",
    );

    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");

    await assertJwtProviderWithJwksUrl(
      page,
      "https://localhost/realms/test2",
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

    await openJwtAuthorizationGrantProvider(page);
    await assertJwtProviderWithPublicKey(
      page,
      "https://localhost/realms/test",
      "keyId",
      "MEMwBQYDK2VxAzoAWOVoLNsZlgw5dvat/Xi83Rh7zQMOerq3XrTT1xVbqDX2naZPlza0gwyNnMV6H6vnUGbaCK/+mgCA",
    );

    await importJwtPublicKeyPemFile(page);
    await assertJwtPublicKeySignatureVerifier(page, /MIIBI/);

    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");

    await importJwtJwksFile(page);
    await assertJwtPublicKeySignatureVerifier(page, /{\s*"keys"\s*:\s*/);

    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");
  });
});
