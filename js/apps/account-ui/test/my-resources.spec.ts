import { test, expect } from "@playwright/test";
import { login } from "./login";

test.describe("My resources page", () => {
  test("List my resources", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "photoz");
    await page.getByTestId("resources").click();
    //await expect(page.getByTestId("row[0].name")).toHaveText("one");
    await expect(page.getByRole("gridcell", { name: "one" })).toBeVisible();
  });
});
