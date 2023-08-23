import { expect } from "@playwright/test";
import { testAccessibility as test } from "./a11y";

test.describe("Accessibility checks for personal info page", async () => {
  test.use({
    url: "./",
  });

  test("accessibility", async ({ runAccessibilityCheck }) => {
    expect(runAccessibilityCheck).toBe("0 errors");
  });
});
