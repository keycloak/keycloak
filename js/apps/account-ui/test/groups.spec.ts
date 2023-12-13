import { test, expect } from "@playwright/test";
import { login } from "./login";

test.describe("Groups page", () => {
  test("List my groups", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "groups");
    await page.getByTestId("groups").click();
    await expect(page.getByTestId("group[0].name")).toHaveText("one");
  });
});
