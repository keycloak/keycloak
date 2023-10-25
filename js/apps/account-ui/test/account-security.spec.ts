import { test, expect } from "@playwright/test";
import { login } from "./login";

test("Check page heading", async ({ page }) => {
  await login(page, "jdoe", "jdoe", "photoz");
  await page
    .getByRole("button", {
      name: "Account security",
    })
    .click();

  const linkedAccountsNavItem = await page.getByTestId(
    "account-security/linked-accounts",
  );

  await linkedAccountsNavItem.isVisible();
  await linkedAccountsNavItem.click();
  await expect(page.getByTestId("page-heading")).toHaveText("Linked accounts");
});
