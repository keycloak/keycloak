import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { expect, test } from "@playwright/test";
import { importUserProfile } from "../admin-client";
import { login } from "../login";
import userProfileConfig from "./user-profile.json" assert { type: "json" };

test.describe("Personal info page", () => {
  test("sets basic information", async ({ page }) => {
    await page.goto("./");
    await page.getByTestId("email").fill("edewit@somewhere.com");
    await page.getByTestId("firstName").fill("Erik");
    await page.getByTestId("lastName").fill("de Wit");
    await page.getByTestId("save").click();

    const alerts = page.getByTestId("alerts");
    await expect(alerts).toHaveText("Your account has been updated.");
  });
});

test.describe("Personal info with userprofile enabled", async () => {
  const realm = "user-profile";

  test.beforeAll(async () => {
    await importUserProfile(userProfileConfig as UserProfileConfig, realm);
  });

  test("render user profile fields", async ({ page }) => {
    await login(page, "jdoe", "jdoe", realm);

    await expect(page.locator("#select")).toBeVisible();
  });
});
