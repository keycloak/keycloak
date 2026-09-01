import { test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickSaveButton, createDefaultTrustProvider } from "./main.ts";
import {
  assertClientIdentityFieldsAreHidden,
  assertDefaultTrustFormDefaults,
  assertDefaultTrustProviderValues,
  assertPublicKeySignatureVerifierFields,
  assertPublicKeySignatureVerifierSaved,
  assertX509TrustMaterialFields,
  disableJwksUrl,
  disableX509TrustMaterial,
  enableX509TrustMaterial,
  fillPublicKeySignatureVerifier,
  fillX509TrustMaterial,
  goToAddDefaultTrustProvider,
  openDefaultTrustProvider,
} from "./default-trust.ts";

const alias = "default-trust";
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
    await goToAddDefaultTrustProvider(page);
    await assertDefaultTrustFormDefaults(page, alias);
    await assertClientIdentityFieldsAreHidden(page);

    await disableJwksUrl(page);
    await assertPublicKeySignatureVerifierFields(page);

    await enableX509TrustMaterial(page);
    await assertX509TrustMaterialFields(page);
  });

  test("should create and edit a Default Trust provider", async ({ page }) => {
    await createDefaultTrustProvider(page, alias, jwksUrl);

    await openDefaultTrustProvider(page, alias);
    await assertDefaultTrustProviderValues(page, jwksUrl);

    await enableX509TrustMaterial(page);
    await fillX509TrustMaterial(page, "stale certificate", "1.2.3.4");
    await disableX509TrustMaterial(page);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");

    await disableJwksUrl(page);
    await fillPublicKeySignatureVerifier(page, jwks);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");
    await assertPublicKeySignatureVerifierSaved(page, jwks);
  });
});
