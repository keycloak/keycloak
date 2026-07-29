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

  test("sorts applications alphabetically", async ({ page }) => {
    await using testBed = await createTestBed();

    await page.route("**/applications", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            clientId: "zebra-client",
            clientName: "Zebra Application",
          },
          {
            clientId: "alpha-client",
            clientName: "Alpha Application",
          },
          {
            clientId: "beta-client",
            clientName: "Beta Application",
          },
        ]),
      });
    });

    await login(page, testBed.realm);
    await page.getByTestId("applications").click();

    const applications = page.getByTestId("applications-list-item");

    await expect(applications).toHaveCount(3);
    await expect(applications.nth(0)).toContainText("Alpha Application");
    await expect(applications.nth(1)).toContainText("Beta Application");
    await expect(applications.nth(2)).toContainText("Zebra Application");
  });
});
