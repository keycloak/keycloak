import { expect, Page } from "@playwright/test";

export async function assertModalTitle(page: Page, title: string) {
  await expect(page.getByText(title, { exact: true })).toBeVisible();
}

export async function assertModalMessage(page: Page, message: string) {
  await expect(page.getByText(message, { exact: true })).toBeVisible();
}

export async function confirmModal(page: Page) {
  await page.getByTestId("confirm").click();
}

export async function cancelModal(page: Page) {
  await page.getByTestId("cancel").click();
}

export async function clickAdd(page: Page) {
  await page.getByTestId("add").click();
}
