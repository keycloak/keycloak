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

  test("ends the session of an application", async ({ page }) => {
    await using testBed = await createTestBed();

    // Mock an application with an active session, the backend behavior is covered by AccountRestServiceTest.
    await page.route("**/applications", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            clientId: "my-application",
            clientName: "My Application",
            inUse: true,
          },
        ]),
      });
    });

    let sessionsDeleted = false;
    await page.route(
      "**/applications/my-application/sessions",
      async (route) => {
        expect(route.request().method()).toBe("DELETE");
        sessionsDeleted = true;
        await route.fulfill({ status: 204 });
      },
    );

    // Log in and navigate to the applications page.
    await login(page, testBed.realm);
    await page.getByTestId("applications").click();

    // The application has an active session, so the action is shown.
    const endSessionButton = page.getByRole("button", {
      name: "End application session",
      exact: true,
    });
    await expect(endSessionButton).toBeVisible();

    // Click the action and confirm the modal.
    await endSessionButton.click();
    await page.getByRole("button", { name: "Confirm", exact: true }).click();

    // Expect a success alert after the sessions have been deleted.
    await expect(page.getByTestId("last-alert")).toContainText(
      "Ended the application session for My Application",
    );
    expect(sessionsDeleted).toBe(true);
  });

  test("does not show the end session action for applications without an active session or for the Account Console itself", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    await page.route("**/applications", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            clientId: "account-console",
            clientName: "Account Console",
            inUse: true,
          },
          {
            clientId: "other-application",
            clientName: "Other Application",
            inUse: false,
          },
        ]),
      });
    });

    // Log in and navigate to the applications page.
    await login(page, testBed.realm);
    await page.getByTestId("applications").click();

    // Neither the Account Console itself nor an application without an active session shows the action.
    await expect(page.getByTestId("applications-list-item")).toHaveCount(2);
    await expect(
      page.getByRole("button", {
        name: "End application session",
        exact: true,
      }),
    ).toHaveCount(0);
  });
});
