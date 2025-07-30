import { expect, test } from "@playwright/test";
import { login } from "../login";

test.describe("Sign out test", () => {
  test("Sign out one device", async ({ browser }) => {
    const context1 = await browser.newContext({
      userAgent:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)",
    });
    const context2 = await browser.newContext();
    try {
      const page1 = await context1.newPage();
      const page2 = await context2.newPage();
      await login(page1, "jdoe", "jdoe", "groups");
      await page1.getByTestId("accountSecurity").click();
      await expect(
        page1.getByTestId("account-security/device-activity"),
      ).toBeVisible();
      await page1.getByTestId("account-security/device-activity").click();
      await expect(page1.getByTestId("row-0")).toContainText("Current session");

      await login(page2, "jdoe", "jdoe", "groups");
      await page2.getByTestId("accountSecurity").click();
      await expect(
        page2.getByTestId("account-security/device-activity"),
      ).toBeVisible();
      await page2.getByTestId("account-security/device-activity").click();

      await page2
        .getByRole("button", { name: "Sign out", exact: true })
        .click();
      await page2.getByRole("button", { name: "Confirm" }).click();

      // reload pages in browsers, one should stay logged in, the other should be logged out
      await page1.reload();
      await page2.reload();
      await expect(
        page1.getByRole("heading", { name: "Sign in to your account" }),
      ).toBeVisible();
      await expect(page2.getByTestId("accountSecurity")).toBeVisible();
    } finally {
      await context1.close();
      await context2.close();
    }
  });

  test("Sign out all devices", async ({ browser }) => {
    const context1 = await browser.newContext({
      userAgent:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)",
    });
    const context2 = await browser.newContext();
    try {
      const page1 = await context1.newPage();
      const page2 = await context2.newPage();
      await login(page1, "jdoe", "jdoe", "groups");
      await login(page2, "jdoe", "jdoe", "groups");

      await page2.getByTestId("accountSecurity").click();
      await page2.getByTestId("account-security/device-activity").click();

      await page2
        .getByRole("button", { name: "Sign out all devices", exact: true })
        .click();
      await page2.getByRole("button", { name: "Confirm" }).click();

      // reload pages in browsers, one should stay logged in, the other should be logged out
      await page1.reload();
      // Reload in page2 should not be needed, as it should be logged out after clicking the button
      await expect(
        page1.getByRole("heading", { name: "Sign in to your account" }),
      ).toBeVisible();
      await expect(
        page2.getByRole("heading", { name: "Sign in to your account" }),
      ).toBeVisible();
    } finally {
      await context1.close();
      await context2.close();
    }
  });
});
