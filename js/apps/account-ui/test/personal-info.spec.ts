import { test, expect } from "@playwright/test";

test.describe("Personal info page", () => {
  test("Setting last and first name", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("personal-info").click();
    await page.getByTestId("firstName").fill("Erik");
    await page.getByTestId("lastName").fill("de Wit");
    await page.getByTestId("save").click();

    const alerts = page.getByTestId("alerts");
    await expect(alerts).toHaveText("Your account has been updated.");
  });
});
