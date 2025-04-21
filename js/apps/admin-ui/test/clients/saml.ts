import { Page, expect } from "@playwright/test";
import { selectItem, switchOff, switchOn } from "../utils/form";

function getTermsOfServiceUrl(page: Page) {
  return page.getByTestId("attributes.tosUri");
}

export async function setTermsOfServiceUrl(page: Page, url: string) {
  await getTermsOfServiceUrl(page).fill(url);
}

export async function saveFineGrain(page: Page) {
  await page.getByTestId("fineGrainSave").click();
}

export async function revertFineGrain(page: Page) {
  await page.getByTestId("fineGrainRevert").click();
}

export async function assertTermsOfServiceUrl(page: Page, expectedUrl: string) {
  await expect(getTermsOfServiceUrl(page)).toHaveValue(expectedUrl);
}

export async function assertSamlClientDetails(page: Page) {
  await expect(page.getByTestId("jump-link-saml-capabilities")).toBeVisible();
}

export async function clickPostBinding(page: Page) {
  await switchOff(page, "#attributes\\.saml🍺force🍺post🍺binding");
}

export async function saveSamlSettings(page: Page) {
  await page.getByTestId("settings-save").click();
}

export async function goToKeysTab(page: Page) {
  await page.getByTestId("keysTab").click();
}

export async function clickClientSignature(page: Page) {
  await switchOff(page, "#clientSignature");
}

export async function assertCertificate(page: Page, exists = true) {
  await expect(page.getByTestId("certificate")).toHaveCount(exists ? 0 : 1);
}

export async function clickEncryptionAssertions(page: Page) {
  await switchOn(page, "#encryptAssertions");
}

export async function clickGenerate(page: Page) {
  await page.getByTestId("generate").click();
}

export async function assertNameIdFormatDropdown(page: Page) {
  const items = ["username", "email", "transient", "persistent"];
  for (const item of items) {
    await selectItem(page, page.locator("#saml_name_id_format"), item);
    await expect(page.locator("#saml_name_id_format")).toHaveText(item);
  }
}
