import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { expect, test } from "@playwright/test";
import resourcesRealm from "./realms/resources-realm.json" with { type: "json" };
import { login } from "./support/actions.ts";
import { createTestBed } from "./support/testbed.ts";
import { waitForRealmReady } from "./support/test-utils.ts";

test.describe("Resources", () => {
  test("shows the resources owned by the user", async ({ page }) => {
    await using testBed = await createTestBed(
      resourcesRealm as RealmRepresentation,
    );
    await waitForRealmReady();

    await login(page, testBed.realm);
    const responsePromise = page.waitForResponse(
      (resp) => resp.url().includes("/realms/") && resp.status() === 200,
    );

    await page.getByTestId("resources").click();
    await responsePromise;

    await expect(page.getByRole("gridcell", { name: "one" })).toBeVisible();
  });

  test("shows no resources are shared with another user", async ({ page }) => {
    await using testBed = await createTestBed(
      resourcesRealm as RealmRepresentation,
    );
    await waitForRealmReady();

    await login(page, testBed.realm, "alice", "alice");
    await page.getByTestId("resources").click();
    await page.getByTestId("sharedWithMe").click();

    const table = page.locator("table");
    await expect(table).toBeVisible();
    const tableData = await page.locator("table > tr").count();
    expect(tableData).toBe(0);
  });

  test("shares a resource with another user", async ({ browser }) => {
    await using testBed = await createTestBed(
      resourcesRealm as RealmRepresentation,
    );
    await waitForRealmReady();

    await using context1 = await browser.newContext();
    await using context2 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();

    // Share a resource as the main user
    await login(page1, testBed.realm);
    await page1.getByTestId("resources").click();

    await page1.getByTestId("expand-one").click();
    await expect(page1.getByText("This resource is not shared.")).toBeVisible();

    await page1.getByTestId("share-one").click();
    await page1.getByTestId("users").click();
    await page1.getByTestId("users").fill("alice");
    await page1.getByTestId("add").click();

    await expect(page1.getByRole("group", { name: "Share with" })).toHaveText(
      "Share with alice",
    );

    await page1
      .getByTestId("permissions")
      .getByRole("button", { expanded: false })
      .click();
    await page1.getByRole("option", { name: "album:view" }).click();
    await page1
      .getByTestId("permissions")
      .getByRole("button", { expanded: true })
      .click();

    await page1.getByTestId("done").click();

    await page1.getByTestId("expand-one").click();
    await expect(page1.getByTestId("shared-with-alice")).toBeVisible();

    // Verify that alice can see the shared resource
    await login(page2, testBed.realm, "alice", "alice");
    await page2.getByTestId("resources").click();

    await page2.getByTestId("sharedWithMe").click();
    const table = page2.locator("table");
    await expect(table).toBeVisible();
    const rowData = page2.getByTestId("row[0].name");
    await expect(rowData).toHaveText("one");
  });

  test("shows an error and no success alert when sharing a resource fails", async ({
    page,
  }) => {
    await using testBed = await createTestBed(
      resourcesRealm as RealmRepresentation,
    );
    await waitForRealmReady();

    await login(page, testBed.realm);
    await page.getByTestId("resources").click();

    await page.getByTestId("expand-one").click();
    await expect(page.getByText("This resource is not shared.")).toBeVisible();

    await page.getByTestId("share-one").click();
    await page.getByTestId("users").click();
    await page.getByTestId("users").fill("alice");
    await page.getByTestId("add").click();

    await page
      .getByTestId("permissions")
      .getByRole("button", { expanded: false })
      .click();
    await page.getByRole("option", { name: "album:view" }).click();
    await page
      .getByTestId("permissions")
      .getByRole("button", { expanded: true })
      .click();

    // Make the share request fail so the console's error handling is exercised.
    await page.route(
      "**/realms/*/account/resources/*/permissions",
      async (route) => {
        if (route.request().method() !== "PUT") {
          return route.continue();
        }
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({
            error: "access_denied",
            error_description: "You are not allowed to share this resource.",
          }),
        });
      },
    );

    await page.getByTestId("done").click();

    await expect(
      page.getByText("You are not allowed to share this resource."),
    ).toBeVisible();
    await expect(page.getByText("Resource successfully shared.")).toBeHidden();
  });
});
