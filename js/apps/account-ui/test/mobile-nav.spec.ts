import { expect, test } from "@playwright/test";
import { login } from "./support/actions.ts";
import { createTestBed } from "./support/testbed.ts";

const MOBILE_VIEWPORT = { width: 400, height: 800 };

test.describe("Mobile nav", () => {
  test("closes the overlay nav after selecting a page", async ({ page }) => {
    await using testBed = await createTestBed();

    await page.setViewportSize(MOBILE_VIEWPORT);
    await login(page, testBed.realm);

    await page.getByTestId("nav-toggle").click();
    await expect(page.locator("#page-sidebar")).toHaveAttribute(
      "aria-hidden",
      "false",
    );

    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/signing-in").click();

    await expect(page.locator("#page-sidebar")).toHaveAttribute(
      "aria-hidden",
      "true",
    );
    await expect(page.getByTestId("password/credential-list")).toBeVisible();
  });

  test("leaves the persistent nav open on desktop viewports", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    // Default project viewport is 1920x1080, above PatternFly's 1200px
    // overlay breakpoint, so the sidebar is a persistent panel.
    await login(page, testBed.realm);

    await expect(page.locator("#page-sidebar")).toHaveAttribute(
      "aria-hidden",
      "false",
    );

    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/signing-in").click();

    await expect(page.locator("#page-sidebar")).toHaveAttribute(
      "aria-hidden",
      "false",
    );
  });
});
