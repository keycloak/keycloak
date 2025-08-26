import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { expect, test } from "@playwright/test";
import resourcesRealm from "./realms/resources-realm.json" with { type: "json" };
import { login } from "./support/actions.ts";
import { createTestBed } from "./support/testbed.ts";

test.describe("Resources", () => {
  test.describe.configure({ mode: "serial" });

  let realm: string;

  test.beforeAll(async () => {
    realm = await createTestBed(resourcesRealm as RealmRepresentation);
  });

  test("shows the resources owned by the user", async ({ page }) => {
    await login(page, realm);
    await page.getByTestId("resources").click();

    await expect(page.getByRole("gridcell", { name: "one" })).toBeVisible();
  });

  test("shows no resources are shared with another user", async ({ page }) => {
    await login(page, realm, "alice", "alice");
    await page.getByTestId("resources").click();

    await page.getByTestId("sharedWithMe").click();
    const tableData = await page.locator("table > tr").count();
    expect(tableData).toBe(0);
  });

  test("shares a recourse with another user", async ({ page }) => {
    await login(page, realm);
    await page.getByTestId("resources").click();

    await page.getByTestId("expand-one").click();
    await expect(page.getByText("This resource is not shared.")).toBeVisible();

    await page.getByTestId("share-one").click();
    await page.getByTestId("users").click();
    await page.getByTestId("users").fill("alice");
    await page.getByTestId("add").click();

    await expect(page.getByRole("group", { name: "Share with" })).toHaveText(
      "Share with alice",
    );

    await page
      .getByTestId("permissions")
      .getByRole("button", { expanded: false })
      .click();
    await page.getByRole("option", { name: "album:view" }).click();
    await page
      .getByTestId("permissions")
      .getByRole("button", { expanded: true })
      .click();

    await page.getByTestId("done").click();

    await page.getByTestId("expand-one").click();
    await expect(page.getByTestId("shared-with-alice")).toBeVisible();
  });

  test("shows the resources shared with another user", async ({ page }) => {
    await login(page, realm, "alice", "alice");
    await page.getByTestId("resources").click();

    await page.getByTestId("sharedWithMe").click();
    const rowData = page.getByTestId("row[0].name");
    await expect(rowData).toHaveText("one");
  });
});
