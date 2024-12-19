import { expect, Locator, Page } from "@playwright/test";

export async function assertRequiredFieldError(page: Page, field: string) {
  await expect(page.getByTestId(field + "-helper")).toHaveText(/required/i);
}

export async function assertFieldError(
  page: Page,
  field: string,
  text: string,
) {
  await expect(page.getByTestId(field + "-helper")).toHaveText(text);
}

export async function selectItem(page: Page, field: Locator, value: string) {
  await field.click();
  await page.getByRole("option", { name: value, exact: true }).click();
}

export async function clickSaveButton(page: Page) {
  await page.getByTestId("save").click();
}
