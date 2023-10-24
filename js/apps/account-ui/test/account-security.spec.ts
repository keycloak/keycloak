import { test, expect } from "@playwright/test";
import { login } from "./login";

test("Check page heading", async ({ page }) => {
  await login(page, "jdoe", "jdoe", "photoz");
  await page
    .getByRole("button", {
      name: "Account security",
    })
    .click();

  const linkedAccountsNavItem = await page.$(
    "a:has-text(/account-security/linked-accounts/)",
  );
  if (linkedAccountsNavItem) {
    await linkedAccountsNavItem.click();
    // Check the page heading
    await expect(page.getByTestId("page-heading")).toHaveText(
      "Linked accounts",
    );
  }
});
