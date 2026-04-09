import { expect, test } from "@playwright/test";
import { login } from "./support/actions.ts";
import { createTestBed } from "./support/testbed.ts";

test.describe("Applications", () => {
  test("shows a list of applications the user has access to", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    // Log in and navigate to the applications page.
    await login(page, testBed.realm);
    await page.getByTestId("applications").click();

    // Assert that the applications list is displayed and contains the expected application.
    await expect(page.getByTestId("applications-list-item")).toHaveCount(1);
    await expect(page.getByTestId("applications-list-item")).toContainText(
      "Account Console",
    );
  });

  test("signs out from an application", async ({ page }) => {
    await using testBed = await createTestBed();

    // Log in and navigate to the applications page.
    await login(page, testBed.realm);
    await page.getByTestId("applications").click();

    // The "Account Console" should be "In use" with a Sign out button visible.
    await expect(page.getByTestId("applications-list-item")).toContainText(
      "In use",
    );
    await expect(
      page.getByRole("button", { name: "Sign out", exact: true }),
    ).toBeVisible();

    // Click "Sign out" for the application.
    await page.getByRole("button", { name: "Sign out", exact: true }).click();

    // Confirm the modal.
    await page.getByRole("button", { name: "Confirm", exact: true }).click();

    // Expect success alert.
    await expect(page.getByTestId("last-alert")).toContainText(
      "Successfully signed out",
    );
  });

  test("does not show sign out button for apps not in use", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    // Log in and navigate to the applications page.
    await login(page, testBed.realm);
    await page.getByTestId("applications").click();

    // Verify the item shows "In use" (the only app is Account Console, which is in use).
    const listItem = page.getByTestId("applications-list-item");
    await expect(listItem).toHaveCount(1);
    await expect(listItem).toContainText("In use");

    // Sign out first to make the app "Not in use".
    await page.getByRole("button", { name: "Sign out", exact: true }).click();
    await page.getByRole("button", { name: "Confirm", exact: true }).click();
    await expect(page.getByTestId("last-alert")).toContainText(
      "Successfully signed out",
    );

    // After refresh, the Sign out button should not be visible for apps not in use.
    await expect(
      page.getByRole("button", { name: "Sign out", exact: true }),
    ).toBeHidden();
  });
});
