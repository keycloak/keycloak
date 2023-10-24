import { test, expect } from "@playwright/test";
import { login } from "./login";

test("Check page heading", async ({ page }) => {
  await login(page, "jdoe", "jdoe", "photoz");
  await page
    .getByRole("button", {
      name: "Account security",
    })
    .click();

  await page.getByTestId("account-security/linked-accounts").click();

  // Check the page heading
  const pageHeadingElement = await page.getByTestId("page-heading");
  await expect(pageHeadingElement).toHaveText("Linked accounts");
});
