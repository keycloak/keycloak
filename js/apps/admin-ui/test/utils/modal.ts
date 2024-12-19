import { expect, Page } from "@playwright/test";

export function checkModalTitle(page: Page, title: string) {
  return expect(page.getByText(title)).toBeVisible();
}

export function confirmModal(page: Page) {
  return page.getByTestId("confirm").click();
}

export async function cancelModal(page: Page) {
  await page.click('[data-testid="cancel"]');
}
