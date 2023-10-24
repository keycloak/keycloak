import { test, expect } from "@playwright/test";
import { login } from "./login";

test("Check page heading", async ({ page }) => {
  await login(page, "jdoe", "jdoe", "photoz");
  const securityButton = await page.getByRole("button", {
    name: "Account security",
  });
  await securityButton.click();

  await page.waitForSelector(
    '[data-testid="account-security/linked-accounts"]',
  );
  const linkedAccountsElement = await page.getByTestId(
    "account-security/linked-accounts",
  );
  await linkedAccountsElement.click();

  // Check the page heading
  await page.waitForSelector('[data-testid="page-heading"]');
  const pageHeadingElement = await page.getByTestId("page-heading");
  const textContent = await pageHeadingElement.innerText();
  expect(textContent).toContain("Linked accounts");
});
