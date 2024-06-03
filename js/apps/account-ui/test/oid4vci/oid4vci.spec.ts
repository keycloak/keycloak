import { expect, test } from "@playwright/test";
import { login } from "../login";

test.describe("Verifiable Credentials page", () => {
  test("Get offer for test-credential.", async ({ page }) => {
    await login(page, "test-user", "test");
    await expect(page.getByTestId("qr-code")).toBeHidden();
    await page.getByTestId("oid4vci").click();
    await page.getByTestId("credential-select").click();
    await expect(
      page.getByTestId("select-verifiable-credential"),
    ).toBeVisible();
    await expect(page.getByTestId("select-natural-person")).toBeVisible();
    await page.getByTestId("select-natural-person").click();
    await expect(page.getByTestId("qr-code")).toBeVisible();
  });
});
