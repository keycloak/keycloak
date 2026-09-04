import { expect, type Locator, type Page } from "@playwright/test";

const LOADING_TIMEOUT_MS = 15_000;
const LOADING_APPEAR_TIMEOUT_MS = 2_000;

/**
 * Waits until no loading spinners are present in the scope.
 * Does not wait on `table-ready` — that marker is only on KeycloakDataTable and
 * can disappear during reload while still briefly visible (race). Use
 * `waitForTableReady` when you know a KeycloakDataTable must be idle.
 */
export async function waitForLoadingComplete(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  const root = scope ?? page;
  const spinner = root.getByTestId("loading-spinner");

  await expect.poll(async () => await spinner.count(), { timeout }).toBe(0);
}

/**
 * Waits for a visible KeycloakDataTable `table-ready` marker and spinner absence.
 * Use only on pages that render KeycloakDataTable, not DraggableTable or static tables.
 */
export async function waitForTableReady(
  page: Page,
  scope?: Locator,
  timeout = LOADING_TIMEOUT_MS,
): Promise<void> {
  const root = scope ?? page;
  const tableReady = root.locator("[data-testid='table-ready']");
  const spinner = root.getByTestId("loading-spinner");

  await expect
    .poll(
      async () => {
        if ((await spinner.count()) > 0) {
          return false;
        }

        const readyCount = await tableReady.count();
        if (readyCount === 0) {
          return true;
        }

        for (let index = 0; index < readyCount; index++) {
          if (await tableReady.nth(index).isVisible()) {
            return true;
          }
        }

        return false;
      },
      { timeout },
    )
    .toBe(true);
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
  const spinner = root.getByTestId("loading-spinner");

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
