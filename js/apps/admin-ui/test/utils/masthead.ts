import { expect, Page } from "@playwright/test";

export async function assertNotificationMessage(page: Page, message: string) {
  await expect(page.getByTestId("last-alert")).toHaveText(message);
}
