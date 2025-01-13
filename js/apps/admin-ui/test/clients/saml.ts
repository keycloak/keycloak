import { Page, expect } from "@playwright/test";

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
