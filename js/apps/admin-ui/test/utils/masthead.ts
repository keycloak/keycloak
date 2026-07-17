import AxeBuilder from "@axe-core/playwright";
import { expect, Page } from "@playwright/test";

const ALERT_TIMEOUT = 15_000;

export async function assertNotificationMessage(page: Page, message: string) {
  const alert = page.getByTestId("last-alert");
  await expect(alert).toBeVisible({ timeout: ALERT_TIMEOUT });
  await expect(alert).toContainText(message, { timeout: ALERT_TIMEOUT });
}

function getActionToggleButton(page: Page) {
  return page.getByTestId("action-dropdown");
}

export async function selectActionToggleItem(page: Page, item: string) {
  await getActionToggleButton(page).click();
  await page.getByRole("menuitem", { name: item, exact: true }).click();
}

export async function assertAxeViolations(page: Page) {
  let { violations } = await new AxeBuilder({ page }).analyze();
  violations = violations.filter(
    (v) => v.impact === "critical" || v.impact === "serious",
  );
  if (violations.length !== 0) console.info(violations);

  expect(violations.length, violations.map((v) => v.help).join("\n")).toBe(0);
}
