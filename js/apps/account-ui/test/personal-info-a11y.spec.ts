import { expect } from "@playwright/test";
import { testAccessibility as test } from "./support/a11y";

test.describe("Accessibility checks for personal info page", async () => {
  test("accessibility", async ({ page, runAccessibilityCheck }) => {
    await page.goto("./");
    expect(runAccessibilityCheck).toBe("0 errors");
  });
});
