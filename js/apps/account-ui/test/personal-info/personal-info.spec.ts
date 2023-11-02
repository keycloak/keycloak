import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { expect, test } from "@playwright/test";
import {
  createUser,
  deleteUser,
  enableLocalization,
  importUserProfile,
} from "../admin-client";
import { login } from "../login";
import userProfileConfig from "./user-profile.json" assert { type: "json" };

const realm = "user-profile";

test.describe("Personal info page", () => {
  test("sets basic information", async ({ page }) => {
    await login(page, "admin", "admin", "master");

    await page.getByTestId("email").fill("edewit@somewhere.com");
    await page.getByTestId("firstName").fill("Erik");
    await page.getByTestId("lastName").fill("de Wit");
    await page.getByTestId("save").click();

    const alerts = page.getByTestId("alerts");
    await expect(alerts).toHaveText("Your account has been updated.");
  });
});

test.describe("Personal info with userprofile enabled", async () => {
  test.beforeAll(async () => {
    await importUserProfile(userProfileConfig as UserProfileConfig, realm);
    await createUser(
      {
        username: "jdoe",
        enabled: true,
        email: "jdoe@keycloak.org",
        firstName: "John",
        lastName: "Doe",
        credentials: [
          {
            type: "password",
            value: "jdoe",
          },
        ],
        realmRoles: [],
        clientRoles: {
          account: ["manage-account"],
        },
      },
      realm,
    );
  });

  test.afterAll(async () => await deleteUser("jdoe", realm));

  test("render user profile fields", async ({ page }) => {
    await login(page, "jdoe", "jdoe", realm);

    await expect(page.locator("#select")).toBeVisible();
    await expect(page.getByTestId("help-label-select")).toBeVisible();
    expect(page.getByText("Alternative email")).toBeDefined();
  });

  test("save user profile", async ({ page }) => {
    await login(page, "jdoe", "jdoe", realm);

    await page.locator("#select").click();
    await page.getByRole("option", { name: "two" }).click();
    await page.getByTestId("email2").fill("non-valid");
    await page.getByTestId("save").click();
    await expect(page.getByTestId("alerts")).toHaveText(
      "Could not update account due to validation errors",
    );

    await expect(page.locator("#email2-helper")).toHaveText(
      "Invalid email address.",
    );

    await page.getByTestId("email2").clear();
    await page.getByTestId("email2").fill("valid@email.com");
    await page.getByTestId("save").click();

    await page.reload();
    await page.locator("delete-account").isVisible();
    await expect(page.getByTestId("email2")).toHaveValue("valid@email.com");
  });
});

// skip currently the locale is not part of the response
test.describe.skip("Realm localization", async () => {
  test.beforeAll(() => enableLocalization("master"));

  test("change locale", async ({ page }) => {
    await login(page, "admin", "admin", "master");
    await page
      .locator("div")
      .filter({ hasText: /^Deutsch$/ })
      .nth(2)
      .click();
    await page.getByRole("option", { name: "English" }).click();
    await page.getByTestId("save").click();
    await page.reload();

    expect(page.locator("div").filter({ hasText: /^English$/ })).toBeDefined();
  });
});
