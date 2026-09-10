import { expect, test } from "@playwright/test";
import { assertLastAlert, login } from "../support/actions.ts";
import { createTestBed } from "../support/testbed.ts";
import { retryOperation, waitForRealmReady } from "../support/test-utils.ts";
import userProfile from "./user-profile.json" with { type: "json" };
import { adminClient } from "../support/admin-client.ts";
import userProfileRealm from "../realms/user-profile-realm.json" with { type: "json" };

test.describe("Personal info", () => {
  test("sets basic information", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, testBed.realm);

    await page.getByTestId("email").fill("edewit@somewhere.com");
    await page.getByTestId("firstName").fill("Erik");
    await page.getByTestId("lastName").fill("de Wit");
    await page.getByTestId("save").click();

    await assertLastAlert(page, "Your account has been updated.");
  });
});

test.describe("Personal info (user profile enabled)", () => {
  test("renders user profile fields", async ({ page }) => {
    await using testBed = await createTestBed(userProfileRealm);
    await waitForRealmReady();

    await retryOperation(() =>
      adminClient.users.updateProfile({
        ...userProfile,
        realm: testBed.realm,
      }),
    );
    await login(page, testBed.realm);

    await expect(page.locator("#select")).toBeVisible();
    await expect(page.getByTestId("help-label-select")).toBeVisible();
    await expect(page.getByText("Alternative email")).toBeVisible();
    await expect(page.getByPlaceholder("Deutsch")).toBeVisible();
    await page.getByTestId("help-label-email2").click();
    await expect(page.getByText("Español")).toBeVisible();
  });

  test("renders long select options as typeahead", async ({ page }) => {
    await using testBed = await createTestBed(userProfileRealm);
    await waitForRealmReady();

    await retryOperation(() =>
      adminClient.users.updateProfile({
        ...userProfile,
        realm: testBed.realm,
      }),
    );
    await login(page, testBed.realm);

    await page.locator("#alternatelang").click();
    await expect(page.getByText("Italiano")).toBeVisible();

    await page.locator("#alternatelang").click();
    await page.locator("*:focus").press("Control+A");
    await page.locator("*:focus").pressSequentially("S");
    await expect(page.getByText("Italiano")).toBeHidden();
    await expect(page.getByText("Slovak")).toBeVisible();
    await expect(page.getByText('Create "S"')).toBeHidden();
  });

  test("renders long list of locales as typeahead", async ({ page }) => {
    await using testBed = await createTestBed(userProfileRealm);
    await waitForRealmReady();

    await retryOperation(() =>
      adminClient.users.updateProfile({
        ...userProfile,
        realm: testBed.realm,
      }),
    );
    await login(page, testBed.realm);

    await page.locator("#attributes\\.locale").click();
    await expect(page.getByText("Italiano")).toBeVisible();

    await page.locator("#attributes\\.locale").click();
    await page.locator("*:focus").press("Control+A");
    await page.locator("*:focus").pressSequentially("S");
    await expect(page.getByText("Italiano")).toBeHidden();
    await expect(page.getByText("Slovak")).toBeVisible();
    await expect(page.getByText('Create "S"')).toBeHidden();
  });

  test("saves user profile", async ({ page }) => {
    await using testBed = await createTestBed(userProfileRealm);
    await waitForRealmReady();

    await retryOperation(() =>
      adminClient.users.updateProfile({
        ...userProfile,
        realm: testBed.realm,
      }),
    );
    await login(page, testBed.realm);

    await page.locator("#select").click();
    await page.getByRole("option", { name: "two" }).click();
    await page.getByTestId("email2").fill("non-valid");
    await page.getByTestId("save").click();
    await assertLastAlert(
      page,
      "Could not update account due to validation errors",
    );

    await expect(page.getByTestId("email2-helper")).toHaveText(
      "Invalid email address.",
    );

    await page.getByTestId("email2").clear();
    await page.getByTestId("email2").fill("valid@email.com");
    await page.getByTestId("save").click();
    await assertLastAlert(page, "Your account has been updated.");

    await page.reload();
    await expect(page.getByTestId("email2")).toHaveValue("valid@email.com");
  });
});

test.describe("Realm localization", () => {
  test("changes locale", async ({ page }) => {
    await using testBed = await createTestBed({
      internationalizationEnabled: true,
      supportedLocales: ["en", "nl", "de"],
    });
    await waitForRealmReady();

    await login(page, testBed.realm);

    await page.locator("#attributes\\.locale").waitFor({ state: "visible" });
    await page.locator("#attributes\\.locale").click();

    await page
      .getByRole("option", { name: "English" })
      .waitFor({ state: "visible" });
    await page.getByRole("option", { name: "English" }).click();

    await page.getByTestId("save").click();
    await assertLastAlert(page, "Your account has been updated.");

    await page.reload();

    await page.locator("#attributes\\.locale").waitFor({ state: "visible" });

    await expect(page.locator("#attributes\\.locale")).toContainText("English");
  });
});
