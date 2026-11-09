import AxeBuilder from "@axe-core/playwright";
import { expect, type Page } from "@playwright/test";

export async function assertNotificationMessage(page: Page, message: string) {
  await expect(page.getByTestId("last-alert")).toHaveText(message);
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
