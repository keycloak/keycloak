import { type Page, expect } from "@playwright/test";

export async function assertRowHasSignOutKebab(page: Page, row: string) {
  await page
    .getByRole("row", { name: row })
    .getByLabel("Kebab toggle")
    .first()
    .click();

  await expect(page.getByRole("menuitem", { name: "Sign out" })).toBeVisible();
}

export async function clickNotBefore(page: Page) {
  await page.getByTestId("clear-not-before-button").click();
}

export async function assertNotBeforeValue(page: Page, value: string) {
  await expect(page.getByTestId("not-before-input")).toHaveValue(value);
}

export async function clickSetToNow(page: Page) {
  await page.getByTestId("set-to-now-button").click();
}

export async function clickPush(page: Page) {
  await page.getByTestId("modal-test-connection-button").click();
}
