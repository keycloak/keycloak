import { expect, test } from "@playwright/test";
import { login } from "./login";
import { getAccountUrl, getAdminUrl, getRootPath } from "./utils";

test.describe("Applications test", () => {
  test.beforeEach(async ({ page }) => {
    // Sign out all devices before each test
    await login(page);
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/device-activity").click();

    await page
      .getByRole("button", { name: "Sign out all devices", exact: true })
      .click();
    await page.getByRole("button", { name: "Confirm" }).click();

    await expect(
      page.getByRole("heading", { name: "Sign in to your account" }),
    ).toBeVisible();
  });

  test("Single application", async ({ page }) => {
    await login(page);

    await page.getByTestId("applications").click();

    await expect(page.getByTestId("applications-list-item")).toHaveCount(1);
    await expect(page.getByTestId("applications-list-item")).toContainText(
      "Account Console",
    );
  });

  test("Single application twice", async ({ browser }) => {
    const context1 = await browser.newContext({
      userAgent:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)",
    });
    const context2 = await browser.newContext();
    try {
      const page1 = await context1.newPage();
      const page2 = await context2.newPage();

      await login(page1);
      await login(page2);

      await page1.getByTestId("applications").click();

      await expect(page1.getByTestId("applications-list-item")).toHaveCount(1);
      await expect(
        page1.getByTestId("applications-list-item").nth(0),
      ).toContainText("Account Console");
    } finally {
      await context1.close();
      await context2.close();
    }
  });

  test("Two applications", async ({ page }) => {
    await login(page);

    // go to admin console
    await page.goto("/");
    await expect(page).toHaveURL(getAdminUrl());
    await page.waitForURL(getAdminUrl());
    await expect(page.getByTestId("options-toggle")).toBeVisible();

    await page.goto(getRootPath());
    await page.waitForURL(getAccountUrl());

    await page.getByTestId("applications").click();

    await expect(page.getByTestId("applications-list-item")).toHaveCount(2);
    await expect(
      page
        .getByTestId("applications-list-item")
        .filter({ hasText: "Account Console" }),
    ).toBeVisible();
    await expect(
      page
        .getByTestId("applications-list-item")
        .filter({ hasText: "security-admin-console" }),
    ).toBeVisible();
  });
});
