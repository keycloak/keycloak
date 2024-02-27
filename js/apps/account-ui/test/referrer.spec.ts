import { expect, test } from "@playwright/test";

import { ADMIN_PASSWORD, ADMIN_USER, DEFAULT_REALM } from "./constants";
import { login } from "./login";
import { getAdminUrl } from "./utils";

test.describe("Signing in with referrer link", () => {
  test("shows a referrer link when a matching client exists", async ({
    page,
  }) => {
    const referrer = "security-admin-console";
    const referrerUrl = getAdminUrl();
    const referrerName = "security admin console";

    const queryParams = {
      referrer,
      referrer_uri: referrerUrl,
    };

    await login(page, ADMIN_USER, ADMIN_PASSWORD, DEFAULT_REALM, queryParams);
    await expect(page.getByTestId("referrer-link")).toContainText(referrerName);

    // Navigate around to ensure the referrer is still shown.
    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    await expect(page.getByTestId("referrer-link")).toContainText(referrerName);
  });

  test("shows no referrer link when an invalid URL is passed", async ({
    page,
  }) => {
    const referrer = "security-admin-console";
    const referrerUrl = "http://i-am-not-an-allowed-url.com";

    const queryParams = {
      referrer,
      referrer_uri: referrerUrl,
    };

    await login(page, ADMIN_USER, ADMIN_PASSWORD, DEFAULT_REALM, queryParams);
    await expect(page.getByText("Manage your basic information")).toBeVisible();
    await expect(page.getByTestId("referrer-link")).toBeHidden();
  });
});
