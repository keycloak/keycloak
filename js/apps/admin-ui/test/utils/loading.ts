import { expect, type Locator, type Page } from "@playwright/test";

const LOADING_TIMEOUT_MS = 15_000;
const LOADING_APPEAR_TIMEOUT_MS = 2_000;

function visibleSpinners(root: Locator): Locator {
  return root.getByTestId("loading-spinner").filter({ visible: true });
}

/**
 * Waits until no visible loading spinners are present in the scope.
 * Hidden KeycloakSpinner instances (for example in collapsed table rows) are ignored.
 */
export async function waitForLoadingComplete(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  const root = scope ?? page;
  const spinner = visibleSpinners(root);

  await expect.poll(async () => await spinner.count(), { timeout }).toBe(0);
}

/**
 * Waits for a loading cycle triggered by a recent interaction (e.g. search Enter).
 * Tolerates a short delay before the spinner appears, then waits until it clears.
 */
export async function waitForLoadingCycle(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  const root = scope ?? page;
  const spinner = visibleSpinners(root);

  try {
    await spinner.first().waitFor({
      state: "visible",
      timeout: LOADING_APPEAR_TIMEOUT_MS,
    });
  } catch {
    // No spinner appeared; table may have updated synchronously or was already idle.
  }

  await waitForLoadingComplete(page, scope, timeout);
}
