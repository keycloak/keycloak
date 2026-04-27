import { expect, test } from "@playwright/test";
import { login } from "./support/actions.ts";
import {
  ADMIN_CLIENT_ID,
  ADMIN_PASSWORD,
  ADMIN_USERNAME,
  DEFAULT_REALM,
  getAdminUrl,
} from "./support/common.ts";

test.describe("Referrer", () => {
  test("shows a referrer link when a matching client exists", async ({
    page,
  }) => {
    const queryParams = new URLSearchParams([
      ["referrer", ADMIN_CLIENT_ID],
      ["referrer_uri", getAdminUrl(DEFAULT_REALM).toString()],
    ]);

    // Log in with a referrer to the admin console, and check if the referrer link is displayed.
    await login(
      page,
      DEFAULT_REALM,
      ADMIN_USERNAME,
      ADMIN_PASSWORD,
      queryParams,
    );
    await expect(page.getByTestId("referrer-link")).toContainText(
      "Security Admin Console",
    );

    // Navigate around and check if the referrer link is still displayed.
    await page.getByTestId("accountSecurity").click();
    await page.getByTestId("account-security/signing-in").click();
    await expect(page.getByTestId("referrer-link")).toContainText(
      "Security Admin Console",
    );
  });

  test("shows no referrer link when an invalid URL is passed", async ({
    page,
  }) => {
    const queryParams = new URLSearchParams([
      ["referrer", ADMIN_CLIENT_ID],
      ["referrer_uri", "http://i-am-not-an-allowed-url.com"],
    ]);

    // Log in with an invalid referrer URL, and check if the referrer link is not displayed.
    await login(
      page,
      DEFAULT_REALM,
      ADMIN_USERNAME,
      ADMIN_PASSWORD,
      queryParams,
    );
    await expect(page.getByText("Manage your basic information")).toBeVisible();
    await expect(page.getByTestId("referrer-link")).toBeHidden();
  });
});
