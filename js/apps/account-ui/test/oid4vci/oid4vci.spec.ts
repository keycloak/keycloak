import { expect, test } from "@playwright/test";
import { login } from "../login";

const realm = "verifiable-credentials";

test.describe("Verifiable Credentials page", () => {
  test("Get offer for test-credential.", async ({ page }) => {
    await login(page, "test-user", "test", realm);

    await expect(page.getByTestId("qr-code")).toBeHidden();

    await page.getByTestId("oid4vci").click();
    await page.getByTestId("menu-toggle").click();

    await expect(page.getByTestId("verifiable-credential")).toBeVisible();
    await expect(page.getByTestId("natural-person")).toBeVisible();
    await page.getByTestId("natural-person").click();
    await expect(page.getByTestId("qr-code")).toBeVisible();
  });
});
