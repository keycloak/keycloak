import AxeBuilder from "@axe-core/playwright";
import { expect, Page } from "@playwright/test";

export async function assertNotificationMessage(page: Page, message: string) {
  await expect(page.getByTestId("last-alert")).toHaveText(message);
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
  if (violations.length !== 0) console.info(violations);

  violations = violations.filter(
    (v) => v.impact === "critical" || v.impact === "serious",
  );
  expect(violations.length, violations.map((v) => v.help).join("\n")).toBe(0);
}
