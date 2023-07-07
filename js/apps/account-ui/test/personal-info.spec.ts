import { test, expect } from "@playwright/test";

test.describe("Personal info page", () => {
  test("sets basic information", async ({ page }) => {
    await page.goto("/");
    await page.getByTestId("email").fill("edewit@example.com");
    await page.getByTestId("firstName").fill("Erik");
    await page.getByTestId("lastName").fill("de Wit");
    await page.getByTestId("save").click();

    const alerts = page.getByTestId("alerts");
    await expect(alerts).toHaveText("Your account has been updated.");
  });
});
