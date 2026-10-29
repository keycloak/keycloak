import { expect, type Page } from "@playwright/test";
import { switchToggle } from "../utils/form.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";

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

  await switchToggle(page, toggle);
  await expect
    .poll(async () => await toggle.isChecked(), { timeout: 10_000 })
    .toBe(!previousState);
  await assertNotificationMessage(page, /changed successfully/i);
}
