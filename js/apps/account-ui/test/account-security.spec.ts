import { test, expect } from "@playwright/test";
import { login } from "./login";

test("Check page heading", async ({ page }) => {
  await login(page, "alice", "alice", "user-profile");
  await page.getByTestId("accountSecurity").click();

  const linkedAccountsNavItem = page.getByTestId(
    "account-security/linked-accounts",
  );

  await expect(linkedAccountsNavItem).toBeVisible();
  await linkedAccountsNavItem.click();
  await expect(page.getByTestId("page-heading")).toHaveText("Linked accounts");
});
