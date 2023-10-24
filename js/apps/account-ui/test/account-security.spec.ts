import { test, expect } from "@playwright/test";
import { login } from "./login";

test.describe("Account security page", () => {
  test("Check linked accounts available", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "photoz");
    await page.getByRole("button", { name: "Account security" }).click();
    const element = page.getByTestId("account-security/linked-accounts");
    const textContent = await element.innerText();
    expect(textContent).toContain("Linked accounts");
  });
});
