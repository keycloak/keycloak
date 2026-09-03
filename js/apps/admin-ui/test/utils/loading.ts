import { expect, type Locator, type Page } from "@playwright/test";

const LOADING_TIMEOUT_MS = 15_000;

export async function waitForLoadingComplete(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  const root = scope ?? page;
  const spinner = root.getByTestId("loading-spinner");
  const busy = root.locator("[aria-busy='true']");

  await expect
    .poll(async () => (await spinner.count()) + (await busy.count()), {
      timeout,
    })
    .toBe(0);
}
