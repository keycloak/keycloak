import { expect, test } from "@playwright/test";
import { login } from "./login";

test.describe("Signing in with referrer link", () => {
  // Tests for keycloak account console, section Signing in in Account security
  test("Should see referrer", async ({ page }) => {
    const queryParams = {
      referrer: "my-app",
      referrer_uri: "http://localhost:3000",
    };
    await login(page, "jdoe", "jdoe", "groups", queryParams);

    await expect(page.getByTestId("referrer-link")).toContainText("my-app");
    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    await expect(page.getByTestId("referrer-link")).toContainText("my-app");
  });

  // Tests for keycloak account console, section Signing in in Account security
  test("Should see no referrer", async ({ page }) => {
    const queryParams = {};
    await login(page, "jdoe", "jdoe", "groups", queryParams);

    await expect(page.getByTestId("referrer-link")).toBeHidden();
    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    await expect(page.getByTestId("referrer-link")).toBeHidden();
  });

  test("Should see no referrer after relogin", async ({ page }) => {
    const queryParams = {
      referrer: "my-app",
      referrer_uri: "http://localhost:3000",
    };
    await login(page, "jdoe", "jdoe", "groups", queryParams);

    await expect(page.getByTestId("referrer-link")).toContainText("my-app");
    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    await expect(page.getByTestId("referrer-link")).toContainText("my-app");

    await page.getByTestId("options").click();
    await page.getByRole("menuitem", { name: "Sign out" }).click();

    const queryParamsNoReferrer = {};
    await login(page, "jdoe", "jdoe", "groups", queryParamsNoReferrer);

    await expect(page.getByTestId("referrer-link")).toBeHidden();
    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    await expect(page.getByTestId("referrer-link")).toBeHidden();
  });
});
