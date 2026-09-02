import { expect, type Locator, type Page } from "@playwright/test";

export async function waitForLoadingComplete(
  page: Page,
  scope?: Locator,
): Promise<void> {
  const root = scope ?? page;
  await expect(root.getByTestId("loading-spinner")).toHaveCount(0);
}
