import { test, expect } from "@playwright/test";
import { login } from "./login";

test.describe("Groups page", () => {
  test("List my groups", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "groups");
    await page.getByTestId("groups").click();
    await expect(page.getByTestId("group[1].name")).toHaveText("three");
  });

  test("List direct and indirect groups", async ({ page }) => {
    await login(page, "alice", "alice", "groups");
    await page.getByTestId("groups").click();

    await expect(
      page.getByTestId("directMembership-checkbox"),
    ).not.toBeChecked();
    await expect(page.getByTestId("group[3].name")).toHaveText("one");
    await expect(
      page.locator("#groups-list li").filter({ hasText: /\/\S+$/ }),
    ).toHaveCount(4);

    await page.getByTestId("directMembership-checkbox").click();
    await expect(page.getByTestId("directMembership-checkbox")).toBeChecked();
    await expect(
      page.locator("#groups-list li").filter({ hasText: /\/\S+$/ }),
    ).toHaveCount(3);
    await expect(page.getByTestId("group[2].name")).toHaveText("subgroup");

    await page.getByTestId("directMembership-checkbox").click();
    await expect(
      page.getByTestId("directMembership-checkbox"),
    ).not.toBeChecked();
    await expect(page.getByTestId("group[3].name")).toHaveText("one");
    await expect(
      page.locator("#groups-list li").filter({ hasText: /\/\S+$/ }),
    ).toHaveCount(4);
  });
});
