import { expect, type Page } from "@playwright/test";
import { SERVER_URL } from "../utils/constants.ts";
import { clickSwitch } from "../utils/form.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";

async function getCurrentRealm(page: Page) {
  return (await page.getByTestId("currentRealm").textContent()) ?? "master";
}

export async function goToAddDefaultTrustProvider(page: Page) {
  const realm = await getCurrentRealm(page);
  await page.goto(
    `${SERVER_URL}/admin/master/console/#/${realm}/identity-providers/default-trust/add`,
  );
}

export async function assertDefaultTrustFormDefaults(
  page: Page,
  alias: string,
) {
  await expect(page.getByTestId("alias")).toHaveValue(alias);
  await expect(page.getByTestId("config.useX509")).not.toBeChecked();
  await expect(page.getByTestId("config.useJwksUrl")).toBeChecked();
  await expect(page.getByTestId("config.jwksUrl")).toBeVisible();
}

export async function assertClientIdentityFieldsAreHidden(page: Page) {
  await expect(page.getByTestId("displayName")).toBeHidden();
  await expect(page.getByTestId("config.clientId")).toBeHidden();
  await expect(page.getByTestId("config.clientSecret")).toBeHidden();
  await expect(page.getByTestId("displayOrder")).toBeHidden();
}

export async function disableJwksUrl(page: Page) {
  await clickSwitch(page, page.getByTestId("config.useJwksUrl"));
}

export async function assertPublicKeySignatureVerifierFields(page: Page) {
  await expect(page.getByTestId("config.jwksUrl")).toBeHidden();
  await expect(
    page.getByTestId("config.publicKeySignatureVerifier"),
  ).toBeVisible();
  await expect(
    page.getByTestId("config.publicKeySignatureVerifierKeyId"),
  ).toBeVisible();
  await expect(page.getByTestId("import-certificate-button")).toBeVisible();
}

export async function enableX509TrustMaterial(page: Page) {
  await clickSwitch(page, page.getByTestId("config.useX509"));
}

export async function disableX509TrustMaterial(page: Page) {
  await clickSwitch(page, page.getByTestId("config.useX509"));
}

export async function assertX509TrustMaterialFields(page: Page) {
  await expect(page.getByTestId("config.useJwksUrl")).toBeHidden();
  await expect(page.getByTestId("config.trustedCertificates")).toBeVisible();
  await expect(
    page.getByTestId("config.requiredExtendedKeyUsages"),
  ).toBeVisible();
}

export async function openDefaultTrustProvider(page: Page, alias: string) {
  await goToIdentityProviders(page);
  await clickTableRowItem(page, alias);
}

export async function assertDefaultTrustProviderValues(
  page: Page,
  jwksUrl: string,
) {
  await expect(page.getByTestId("config.useJwksUrl")).toBeChecked();
  await expect(page.getByTestId("config.jwksUrl")).toHaveValue(jwksUrl);
  await expect(page.getByTestId("displayName")).toBeHidden();
  await expect(page.getByTestId("config.clientId")).toBeHidden();
  await expect(page.getByTestId("config.clientSecret")).toBeHidden();
  await expect(page.getByTestId("mappers-tab")).toBeHidden();
}

export async function fillX509TrustMaterial(
  page: Page,
  certificate: string,
  requiredExtendedKeyUsages: string,
) {
  await page.getByTestId("config.trustedCertificates").fill(certificate);
  await page
    .getByTestId("config.requiredExtendedKeyUsages")
    .fill(requiredExtendedKeyUsages);
}

export async function fillPublicKeySignatureVerifier(page: Page, key: string) {
  await page.getByTestId("config.publicKeySignatureVerifier").fill(key);
}

export async function assertPublicKeySignatureVerifierSaved(
  page: Page,
  key: string,
) {
  await expect(page.getByTestId("config.useJwksUrl")).not.toBeChecked();
  await expect(
    page.getByTestId("config.publicKeySignatureVerifier"),
  ).toHaveValue(key);
}
