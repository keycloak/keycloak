import { test, expect } from "@playwright/test";
import { login } from "./login";

test.describe("My resources page", () => {
  test("List my resources", async ({ page }) => {
    await page.goto("/?realm=photoz");
    login(page, "jdoe", "jdoe");
    await page.waitForURL("/?realm=photoz");
    await page.getByTestId("resources").click();
    await expect(page.getByRole("gridcell", { name: "one" })).toBeVisible();
  });
});
