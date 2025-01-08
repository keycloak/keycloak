import { expect, Page } from "@playwright/test";

export function assertModalTitle(page: Page, title: string) {
  return expect(page.getByText(title)).toBeVisible();
}

export function confirmModal(page: Page) {
  return page.getByTestId("confirm").click();
}

export async function cancelModal(page: Page) {
  await page.getByTestId("cancel").click();
}
