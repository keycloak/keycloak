import { type Locator, type Page, expect } from "@playwright/test";
import {
  assertSelectValue,
  selectItem,
  switchOff,
  switchOn,
} from "../utils/form.ts";

const REALM_ACTIVE_KEY_TEXT = "Use default (realm's active signing key)";

function getTermsOfServiceUrl(page: Page) {
  return page.getByTestId("attributes.tosUri");
}

function getKeyForEncryptionAlgorithmInput(page: Page) {
  return page.locator("#attributes\\.saml🍺encryption🍺algorithm");
}

function getKeyForEncryptionKeyAlgorithmInput(page: Page) {
  return page.locator("#attributes\\.saml🍺encryption🍺keyAlgorithm");
}

function getKeyForEncryptionDigestMethodInput(page: Page) {
  return page.locator("#attributes\\.saml🍺encryption🍺digestMethod");
}

function getKeyForEncryptionMaskGenerationFunctionInput(page: Page) {
  return page.locator("#attributes\\.saml🍺encryption🍺maskGenerationFunction");
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

export async function goToClientSettingsTab(page: Page) {
  await page.getByTestId("clientSettingsTab").click();
}

export async function clickClientSignature(page: Page) {
  await switchOff(page, "#clientSignature");
}

// Assert that the number of certificates enabled matches the number of certificates displayed
export async function assertCertificates(page: Page) {
  const certsEnabled = [
    await page.getByTestId("encryptAssertions").isChecked(),
    await page.getByTestId("clientSignature").isChecked(),
  ].filter(Boolean).length;

  await expect(page.getByTestId("certificate")).toHaveCount(certsEnabled);
}

export async function clickEncryptionAssertions(page: Page) {
  await switchOn(page, "#encryptAssertions");
}

export async function clickOffEncryptionAssertions(page: Page) {
  await switchOff(page, "#encryptAssertions");
}

export async function clickGenerate(page: Page) {
  const responsePromise = page.waitForResponse(
    (res) => res.url().includes("/generate") && res.status() === 200,
    { timeout: 60000 },
  );
  await page.getByTestId("generate").click();
  await responsePromise;
}

export async function assertNameIdFormatDropdown(page: Page) {
  const items = ["username", "email", "transient", "persistent"];
  for (const item of items) {
    await selectItem(
      page,
      page.locator("#attributes\\.saml_name_id_format"),
      item,
    );
    await expect(page.locator("#attributes\\.saml_name_id_format")).toHaveText(
      item,
    );
  }
}

export async function selectEncryptionAlgorithmInput(
  page: Page,
  value: string,
) {
  await selectItem(page, getKeyForEncryptionAlgorithmInput(page), value);
}

export async function selectEncryptionKeyAlgorithmInput(
  page: Page,
  value: string,
) {
  await selectItem(page, getKeyForEncryptionKeyAlgorithmInput(page), value);
}

export async function selectEncryptionDigestMethodInput(
  page: Page,
  value: string,
) {
  await selectItem(page, getKeyForEncryptionDigestMethodInput(page), value);
}

export async function selectEncryptionMaskGenerationFunctionInput(
  page: Page,
  value: string,
) {
  await selectItem(
    page,
    getKeyForEncryptionMaskGenerationFunctionInput(page),
    value,
  );
}

export async function assertEncryptionAlgorithm(page: Page, value: string) {
  await assertSelectValue(getKeyForEncryptionAlgorithmInput(page), value);
}

export async function assertEncryptionKeyAlgorithm(page: Page, value: string) {
  await assertSelectValue(getKeyForEncryptionKeyAlgorithmInput(page), value);
}

export async function assertEncryptionDigestMethod(page: Page, value: string) {
  await assertSelectValue(getKeyForEncryptionDigestMethodInput(page), value);
}

export async function assertEncryptionMaskGenerationFunction(
  page: Page,
  value: string,
) {
  await assertSelectValue(
    getKeyForEncryptionMaskGenerationFunctionInput(page),
    value,
  );
}

async function assertInputVisible(locator: Locator, visible: boolean) {
  if (visible) {
    await expect(locator).toBeVisible();
  } else {
    await expect(locator).toBeHidden();
  }
}

export async function assertEncryptionAlgorithmInputVisible(
  page: Page,
  visible: boolean,
) {
  await assertInputVisible(getKeyForEncryptionAlgorithmInput(page), visible);
}

export async function assertEncryptionKeyAlgorithmInputVisible(
  page: Page,
  visible: boolean,
) {
  await assertInputVisible(getKeyForEncryptionKeyAlgorithmInput(page), visible);
}

export async function assertEncryptionDigestMethodInputVisible(
  page: Page,
  visible: boolean,
) {
  await assertInputVisible(getKeyForEncryptionDigestMethodInput(page), visible);
}

export async function assertEncryptionMaskGenerationFunctionInputVisible(
  page: Page,
  visible: boolean,
) {
  await assertInputVisible(
    getKeyForEncryptionMaskGenerationFunctionInput(page),
    visible,
  );
}

function getSigningKeyIdSelect(page: Page) {
  return page.locator("#attributes\\.saml🍺server🍺signature🍺kid");
}

export async function assertSigningKeyDropdownVisible(page: Page) {
  await expect(getSigningKeyIdSelect(page)).toBeVisible();
}

export async function selectSigningKey(page: Page, keyId: string) {
  const field = getSigningKeyIdSelect(page);
  await field.click();
  // Find option that contains the keyId (format: "algorithm - kid")
  await page
    .getByRole("option")
    .filter({ hasText: new RegExp(`- ${keyId}$`) })
    .click();
}

export async function assertSigningKeyValue(page: Page, expectedValue: string) {
  const field = getSigningKeyIdSelect(page);
  if (expectedValue === "") {
    await expect(field).toHaveText(REALM_ACTIVE_KEY_TEXT);
  } else {
    await expect(field).toHaveText(new RegExp(`- ${expectedValue}$`));
  }
}

export async function assertSamlSigningKeyDisplayText(
  page: Page,
  expectedText: string | RegExp,
) {
  await expect(getSigningKeyIdSelect(page)).toHaveText(expectedText);
}
