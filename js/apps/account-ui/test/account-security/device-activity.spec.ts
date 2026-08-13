import { expect, test } from "@playwright/test";
import { login } from "../support/actions.ts";
import { createTestBed } from "../support/testbed.ts";

test.describe("Device activity", () => {
  test("signs out of a single device session", async ({ browser }) => {
    await using testBed = await createTestBed();
    await using context1 = await browser.newContext();
    await using context2 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();

    // Log in the first session, and verify it is active.
    await login(page1, testBed.realm);
    await page1.getByTestId("accountSecurity").click();
    await page1.getByTestId("account-security/device-activity").click();
    await expect(page1.getByTestId("row-0")).toContainText("Current session");

    // Log in the second session, and verify it is active.
    await login(page2, testBed.realm);
    await page2.getByTestId("accountSecurity").click();
    await page2.getByTestId("account-security/device-activity").click();
    await expect(page2.getByTestId("row-0")).toContainText("Current session");

    // Sign out the first session from the second session.
    await page2.getByRole("button", { name: "Sign out", exact: true }).click();
    await page2.getByRole("button", { name: "Confirm", exact: true }).click();
    await expect(page2.getByTestId("last-alert")).toContainText("Signed out");

    // Reload pages and verify the first session is logged out, while the second session remains active.
    await page1.reload();
    await page2.reload();
    await expect(
      page1.getByRole("heading", {
        name: "Sign in to your account",
        exact: true,
      }),
    ).toBeVisible();
    await expect(page2.getByTestId("accountSecurity")).toBeVisible();
  });

  test("signs out of all device sessions", async ({ browser }) => {
    await using testBed = await createTestBed();
    await using context1 = await browser.newContext();
    await using context2 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();

    // Log in both sessions, then sign out of all devices from the second session.
    await login(page1, testBed.realm);
    await login(page2, testBed.realm);
    await page2.getByTestId("accountSecurity").click();
    await page2.getByTestId("account-security/device-activity").click();
    await page2
      .getByRole("button", { name: "Sign out all devices", exact: true })
      .click();
    await page2.getByRole("button", { name: "Confirm", exact: true }).click();
    await expect(
      page2.getByRole("heading", {
        name: "Sign in to your account",
        exact: true,
      }),
    ).toBeVisible();

    // Reload only the first page (second page is already logged out), and verify both sessions are logged out.
    await page1.reload();
    await expect(
      page1.getByRole("heading", {
        name: "Sign in to your account",
        exact: true,
      }),
    ).toBeVisible();
  });
});
