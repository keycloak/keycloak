import { expect, type Page } from "@playwright/test";
import { chooseFileByLocator } from "../utils/file-chooser.ts";
import { selectItem } from "../utils/form.ts";
import { assertModalTitle, confirmModal } from "../utils/modal.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";

export async function openJwtAuthorizationGrantProvider(
  page: Page,
  alias = "jwt-authorization-grant",
) {
  await goToIdentityProviders(page);
  await clickTableRowItem(page, alias);
}

export async function assertJwtProviderWithJwksUrl(
  page: Page,
  issuer: string,
  jwksUrl: string,
) {
  await expect(page.getByTestId("config.issuer")).toHaveValue(issuer);
  await expect(page.getByTestId("config.useJwksUrl")).toBeChecked();
  await expect(page.getByTestId("config.jwksUrl")).toHaveValue(jwksUrl);
}

export async function fillJwtProviderWithJwksUrl(
  page: Page,
  issuer: string,
  jwksUrl: string,
) {
  await page.getByTestId("config.issuer").fill(issuer);
  await page.getByTestId("config.jwksUrl").fill(jwksUrl);
}

export async function assertJwtProviderWithPublicKey(
  page: Page,
  issuer: string,
  keyId: string,
  key: string,
) {
  await expect(page.getByTestId("config.issuer")).toHaveValue(issuer);
  await expect(page.getByTestId("config.useJwksUrl")).not.toBeChecked();
  await expect(page.getByTestId("config.jwksUrl")).toBeHidden();
  await expect(
    page.getByTestId("config.publicKeySignatureVerifierKeyId"),
  ).toHaveValue(keyId);
  await expect(
    page.getByTestId("config.publicKeySignatureVerifier"),
  ).toHaveValue(key);
}

export async function assertJwtPublicKeySignatureVerifier(
  page: Page,
  value: string | RegExp,
) {
  await expect(
    page.getByTestId("config.publicKeySignatureVerifier"),
  ).toHaveValue(value);
}

async function openImportKeyDialog(page: Page) {
  await page.getByTestId("import-certificate-button").click();
  await assertModalTitle(page, "Import key");
}

export async function importJwtPublicKeyPemFile(page: Page) {
  await openImportKeyDialog(page);
  await selectItem(page, page.locator("#keystoreFormat"), "Public Key PEM");
  await chooseFileByLocator(
    page,
    "../utils/files/key.pem",
    page.locator("#importFile-browse-button"),
  );
  await confirmModal(page);
}

export async function importJwtJwksFile(page: Page) {
  await openImportKeyDialog(page);
  await selectItem(page, page.locator("#keystoreFormat"), "JSON Web Key Set");
  await chooseFileByLocator(
    page,
    "../utils/files/key.jwks",
    page.locator("#importFile-browse-button"),
  );
  await confirmModal(page);
}
