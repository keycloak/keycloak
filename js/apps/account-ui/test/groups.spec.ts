import { test, expect } from "@playwright/test";
import { login } from "./login";

test.describe("Groups page", () => {
  test("List my groups", async ({ page }) => {
    await page.goto("/?realm=groups");
    login(page, "jdoe", "jdoe");
    await page.waitForURL("/?realm=groups");
    await page.getByTestId("groups").click();
    await expect(page.getByTestId("group[0].name")).toHaveText("one");
  });
});
