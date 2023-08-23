import { expect } from "@playwright/test";
import { testAccessibility as test } from "./support/a11y";

test.describe("Accessibility checks for groups page", async () => {
  test("Accessibility checks", async ({ page, runAccessibilityCheck }) => {
    await page.goto("./");
    await page.getByTestId("groups").click();
    await page.waitForLoadState("load");

    expect(runAccessibilityCheck).toBe("0 errors");
  });
});
