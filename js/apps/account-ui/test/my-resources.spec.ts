import { test, expect } from "@playwright/test";
import { login } from "./login";

test.describe("My resources page", () => {
  test.describe.configure({ mode: "serial" });

  test("List my resources", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "photoz");
    await page.getByTestId("resources").click();

    await expect(page.getByRole("gridcell", { name: "one" })).toBeVisible();
  });

  test("Nothing is shared with alice", async ({ page }) => {
    await login(page, "alice", "alice", "photoz");
    await page.getByTestId("resources").click();

    await page.getByTestId("sharedWithMe").click();
    const tableData = await page.locator("table > tr").count();
    expect(tableData).toBe(0);
  });

  test("Share one with alice", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "photoz");
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

  test("One is shared with alice", async ({ page }) => {
    await login(page, "alice", "alice", "photoz");
    await page.getByTestId("resources").click();

    await page.getByTestId("sharedWithMe").click();
    const rowData = page.getByTestId("row[0].name");
    await expect(rowData).toHaveText("one");
  });
});
