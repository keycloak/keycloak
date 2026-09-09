import { expect, type Locator, type Page } from "@playwright/test";

const LOADING_TIMEOUT_MS = 15_000;
const LOADING_APPEAR_TIMEOUT_MS = 2_000;

function tableLoadingOverlays(root: Locator): Locator {
  return root.getByTestId("table-loading-overlay").filter({ visible: true });
}

function pageLoadingSpinners(root: Locator): Locator {
  return root.getByTestId("page-loading-spinner").filter({ visible: true });
}

/**
 * Waits until no visible table loading overlays are present in the scope.
 */
export async function waitForLoadingComplete(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  const root = scope ?? page;
  const overlay = tableLoadingOverlays(root);

  await expect.poll(async () => await overlay.count(), { timeout }).toBe(0);
}

/**
 * Waits until no visible full-page KeycloakSpinner markers are present.
 */
export async function waitForPageLoadingComplete(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  const root = scope ?? page;
  const spinner = pageLoadingSpinners(root);

  await expect.poll(async () => await spinner.count(), { timeout }).toBe(0);
}

/**
 * Waits for both table overlay and page-level loading indicators to clear.
 */
export async function waitForTableIdle(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  await waitForLoadingComplete(page, scope, timeout);
  await waitForPageLoadingComplete(page, scope, timeout);
}

/**
 * Waits for a loading cycle triggered by a recent interaction (e.g. search Enter).
 * Tolerates a short delay before the overlay appears, then waits until it clears.
 */
export async function waitForLoadingCycle(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  const root = scope ?? page;
  const overlay = tableLoadingOverlays(root);

  try {
    await overlay.first().waitFor({
      state: "visible",
      timeout: LOADING_APPEAR_TIMEOUT_MS,
    });
  } catch {
    // No overlay appeared; table may have updated synchronously or was already idle.
  }

  await waitForLoadingComplete(page, scope, timeout);
}
