import AxeBuilder from "@axe-core/playwright";
import { expect, type Page } from "@playwright/test";

const ALERT_TIMEOUT = 15_000;

export async function assertNotificationMessage(
  page: Page,
  message: string | RegExp,
) {
  const alert = page.getByTestId("last-alert");
  await expect(alert).toBeVisible({ timeout: ALERT_TIMEOUT });
  await expect(alert).toHaveText(message, { timeout: ALERT_TIMEOUT });
  // Close the toast so a later assertion for the same message cannot match this stale one.
  // A CSS locator is used as role-based locators ignore content hidden by an open modal.
  await alert
    .locator("button")
    .click({ timeout: 1_000 })
    .catch(() => {
      // The toast may have expired on its own between the assertion and the click
    });
}

function getActionToggleButton(page: Page) {
  return page.getByTestId("action-dropdown");
}

export async function selectActionToggleItem(page: Page, item: string) {
  await getActionToggleButton(page).click();
  const menuItem = page.getByRole("menuitem", { name: item, exact: true });
  try {
    await expect(menuItem).toBeVisible({ timeout: 5_000 });
  } catch (cause) {
    throw new Error(
      `Menu item "${item}" did not appear after clicking the action dropdown. ` +
        "The dropdown of the previous page might have been clicked while navigation was still in progress: " +
        "wait for the new page (for example its view-header) to be visible before calling this helper.",
      { cause },
    );
  }
  await menuItem.click();
}

export async function assertAxeViolations(page: Page) {
  let { violations } = await new AxeBuilder({ page }).analyze();
  violations = violations.filter(
    (v) => v.impact === "critical" || v.impact === "serious",
  );
  if (violations.length !== 0) console.info(violations);

  expect(violations.length, violations.map((v) => v.help).join("\n")).toBe(0);
}
