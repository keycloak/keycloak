import { type Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";

export async function goToOTPPolicyTab(page: Page) {
  await page.getByTestId("otpPolicy").click();
}

export async function assertSupportedApplications(
  page: Page,
  applications: string[],
) {
  const supportedApplications = page.getByTestId("supportedApplications");
  await expect(supportedApplications).toHaveText(applications.join(""));
}

export async function setPolicyType(page: Page, type: string) {
  await page.getByTestId(type).click();
}

export async function increaseInitialCounter(page: Page) {
  await page.locator("#otpPolicyInitialCounter").getByLabel("Plus").click();
}

export async function goToWebauthnPage(page: Page) {
  await page.getByTestId("webauthnPolicy").click();
}

export async function goToWebauthnPasswordlessPage(page: Page) {
  await page.getByTestId("webauthnPasswordlessPolicy").click();
}

export async function fillSelects(page: Page, data: Record<string, string>) {
  for (const prop of Object.keys(data)) {
    const select = page.locator(`#${prop}`);
    await selectItem(page, select, data[prop]);
  }
}

export async function setWebAuthnPolicyCreateTimeout(
  page: Page,
  value: number,
) {
  await page.getByTestId("webAuthnPolicyCreateTimeout").fill(String(value));
}
