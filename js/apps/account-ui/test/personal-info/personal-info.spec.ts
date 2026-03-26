import { expect, test } from "@playwright/test";
import { assertLastAlert, login } from "../support/actions.ts";
import { createTestBed } from "../support/testbed.ts";
import userProfile from "./user-profile.json" with { type: "json" };
import { adminClient } from "../support/admin-client.ts";
import userProfileRealm from "../realms/user-profile-realm.json" with { type: "json" };

/**
 * Retry with backoff after maximum of 5 retries.
 */
async function retryOperation<T>(
  operation: () => Promise<T>,
  maxRetries = 5,
  initialDelay = 100,
): Promise<T> {
  let lastError: Error | undefined;

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error as Error;

      // Only retry on 500 errors (server errors), not on validation errors
      if (error instanceof Error && error.message.includes("unknown_error")) {
        const delay = initialDelay * Math.pow(2, attempt);
        await new Promise((resolve) => setTimeout(resolve, delay));
        continue;
      }

      // For other errors, throw immediately
      throw error;
    }
  }

  throw lastError;
}

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

    await retryOperation(() =>
      adminClient.users.updateProfile({
        ...userProfile,
        realm: testBed.realm,
      }),
    );
    await login(page, testBed.realm);

    await expect(page.locator("#select")).toBeVisible();
    await expect(page.getByTestId("help-label-select")).toBeVisible();
    await expect(page.getByText("Alternative email")).toHaveCount(1);
    await expect(page.getByPlaceholder("Deutsch")).toHaveCount(1);
    await page.getByTestId("help-label-email2").click();
    await expect(page.getByText("Español")).toHaveCount(1);
  });

  test("renders long select options as typeahead", async ({ page }) => {
    await using testBed = await createTestBed(userProfileRealm);

    await retryOperation(() =>
      adminClient.users.updateProfile({
        ...userProfile,
        realm: testBed.realm,
      }),
    );
    await login(page, testBed.realm);

    await page.locator("#alternatelang").click();
    await page.waitForSelector("text=Italiano");

    await page.locator("#alternatelang").click();
    await page.locator("*:focus").press("Control+A");
    await page.locator("*:focus").pressSequentially("S");
    await expect(page.getByText("Italiano")).toHaveCount(0);
    await expect(page.getByText("Slovak")).toBeVisible();
    await expect(page.getByText('Create "S"')).toBeHidden();
  });

  test("renders long list of locales as typeahead", async ({ page }) => {
    await using testBed = await createTestBed(userProfileRealm);

    await retryOperation(() =>
      adminClient.users.updateProfile({
        ...userProfile,
        realm: testBed.realm,
      }),
    );
    await login(page, testBed.realm);

    await page.locator("#attributes\\.locale").click();
    await page.waitForSelector("text=Italiano");

    await page.locator("#attributes\\.locale").click();
    await page.locator("*:focus").press("Control+A");
    await page.locator("*:focus").pressSequentially("S");
    await expect(page.getByText("Italiano")).toHaveCount(0);
    await expect(page.getByText("Slovak")).toBeVisible();
    await expect(page.getByText('Create "S"')).toBeHidden();
  });

  test("saves user profile", async ({ page }) => {
    await using testBed = await createTestBed(userProfileRealm);

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
    await page.locator("delete-account").isVisible();
    await expect(page.getByTestId("email2")).toHaveValue("valid@email.com");
  });
});

test.describe("Realm localization", () => {
  test("changes locale", async ({ page }) => {
    await using testBed = await createTestBed({
      internationalizationEnabled: true,
      supportedLocales: ["en", "nl", "de"],
    });

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
