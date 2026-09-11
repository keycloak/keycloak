import { expect, Page } from "@playwright/test";

export async function assertModalTitle(page: Page, title: string) {
  await expect(page.getByText(title, { exact: true })).toBeVisible();
}

export async function assertModalMessage(page: Page, message: string) {
  await expect(page.getByText(message, { exact: true })).toBeVisible();
}

export async function confirmModal(page: Page) {
  const confirm = page.getByTestId("confirm");
  await expect(confirm).toBeVisible();
  await confirm.click();
}

export async function cancelModal(page: Page) {
  const cancel = page.getByTestId("cancel");
  await expect(cancel).toBeVisible();
  await cancel.click();
}

export async function clickAdd(page: Page) {
  await page.getByTestId("add").click();
}
