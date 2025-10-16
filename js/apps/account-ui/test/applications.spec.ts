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
});
