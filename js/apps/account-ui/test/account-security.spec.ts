import { test, expect } from "@playwright/test";
import { login } from "./login";

test("Check page heading", async ({ page }) => {
  await login(page, "alice", "alice", "user-profile");
  await page
    .getByRole("button", {
      name: "Account security",
    })
    .click();

  const linkedAccountsNavItem = page.getByTestId(
    "account-security/linked-accounts",
  );

  expect(linkedAccountsNavItem).toBeVisible();
  await linkedAccountsNavItem.click();
  await expect(page.getByTestId("page-heading")).toHaveText("Linked accounts");
});
