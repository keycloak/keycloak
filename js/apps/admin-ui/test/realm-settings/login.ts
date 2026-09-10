import { expect, type Page } from "@playwright/test";
import { switchToggle } from "../utils/form.ts";

export async function goToLoginTab(page: Page) {
  await page.getByTestId("rs-login-tab").click();
}

export async function toggleLoginSettingAndExpectSuccess(
  page: Page,
  switchTestId: string,
) {
  const toggle = page.getByTestId(switchTestId);
  await expect(toggle).toBeVisible();
  const previousState = await toggle.isChecked();

  const previousAlert = page.getByTestId("last-alert");
  if (await previousAlert.isVisible()) {
    await previousAlert.locator("button").first().click();
    await expect(previousAlert).toBeHidden();
  }

  await switchToggle(page, toggle);
  await expect
    .poll(async () => await toggle.isChecked(), { timeout: 10_000 })
    .toBe(!previousState);
  await expect(page.getByTestId("last-alert")).toContainText(
    /changed successfully/i,
  );
}
