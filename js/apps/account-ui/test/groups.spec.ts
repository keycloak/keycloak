import { expect, test } from "@playwright/test";
import groupsRealm from "./realms/groups-realm.json" with { type: "json" };
import { login } from "./support/actions.ts";
import { createTestBed } from "./support/testbed.ts";

test.describe("Groups", () => {
  test("lists groups", async ({ page }) => {
    await using testBed = await createTestBed(groupsRealm);

    await login(page, testBed.realm);
    await page.getByTestId("groups").click();
    await expect(page.getByTestId("group[1].name")).toHaveText("three");
  });

  test("lists direct and indirect groups", async ({ page }) => {
    await using testBed = await createTestBed(groupsRealm);

    await login(page, testBed.realm, "alice", "alice");
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
