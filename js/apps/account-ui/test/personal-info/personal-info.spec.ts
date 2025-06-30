import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { expect, test } from "@playwright/test";
import {
  createRandomUserWithPassword,
  deleteUser,
  enableLocalization,
  importUserProfile,
  inRealm,
} from "../admin-client";
import { login } from "../login";
import userProfileConfig from "./user-profile.json" assert { type: "json" };
import { randomUUID } from "crypto";

const realm = "user-profile";

test.describe("Personal info page", () => {
  let user: string;
  test("sets basic information", async ({ page }) => {
    user = await createRandomUserWithPassword("user-" + randomUUID(), "pwd");

    await login(page, user, "pwd");

    await page.getByTestId("email").fill(`${user}@somewhere.com`);
    await page.getByTestId("firstName").fill("Erik");
    await page.getByTestId("lastName").fill("de Wit");
    await page.getByTestId("save").click();

    const alerts = page.getByTestId("alerts");
    await expect(alerts).toHaveText("Your account has been updated.");
  });
});

test.describe("Personal info with userprofile enabled", () => {
  let user: string;
  test.beforeAll(async () => {
    await importUserProfile(userProfileConfig as UserProfileConfig, realm);
    user = await inRealm(realm, () =>
      createRandomUserWithPassword("user-" + randomUUID(), "jdoe", {
        email: "jdoe@keycloak.org",
        firstName: "John",
        lastName: "Doe",
        realmRoles: [],
        clientRoles: {
          account: ["manage-account"],
        },
      }),
    );
  });

  test.afterAll(async () => await inRealm(realm, () => deleteUser(user)));

  test("render user profile fields", async ({ page }) => {
    await login(page, user, "jdoe", realm);

    await expect(page.locator("#select")).toBeVisible();
    await expect(page.getByTestId("help-label-select")).toBeVisible();
    await expect(page.getByText("Alternative email")).toHaveCount(1);
    await expect(page.getByPlaceholder("Deutsch")).toHaveCount(1);
    await page.getByTestId("help-label-email2").click();
    await expect(page.getByText("Español")).toHaveCount(1);
  });

  test("render long select options as typeahead", async ({ page }) => {
    await login(page, user, "jdoe", realm);

    await page.locator("#alternatelang").click();
    await page.waitForSelector("text=Italiano");

    await page.locator("#alternatelang").click();
    await page.locator("*:focus").press("Control+A");
    await page.locator("*:focus").pressSequentially("S");
    await expect(page.getByText("Italiano")).toHaveCount(0);
    await expect(page.getByText("Slovak")).toBeVisible();
    await expect(page.getByText('Create "S"')).toBeHidden();
  });

  test("render long list of locales as typeahead", async ({ page }) => {
    await login(page, user, "jdoe", realm);

    await page.locator("#locale").click();
    await page.waitForSelector("text=Italiano");

    await page.locator("#locale").click();
    await page.locator("*:focus").press("Control+A");
    await page.locator("*:focus").pressSequentially("S");
    await expect(page.getByText("Italiano")).toHaveCount(0);
    await expect(page.getByText("Slovak")).toBeVisible();
    await expect(page.getByText('Create "S"')).toBeHidden();
  });

  test("save user profile", async ({ page }) => {
    await login(page, user, "jdoe", realm);

    await page.locator("#select").click();
    await page.getByRole("option", { name: "two" }).click();
    await page.getByTestId("email2").fill("non-valid");
    await page.getByTestId("save").click();
    await expect(page.getByTestId("alerts")).toHaveText(
      "Could not update account due to validation errors",
    );

    await expect(page.getByTestId("email2-helper")).toHaveText(
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
test.describe.skip("Realm localization", () => {
  test.beforeAll(() => enableLocalization());

  test("change locale", async ({ page }) => {
    const user = await createRandomUserWithPassword(
      "user-" + randomUUID(),
      "pwd",
    );

    await login(page, user, "pwd");
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
