import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertAxeViolations } from "../utils/masthead.ts";
import { goToRealm, goToRealmRoles } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";

test.describe.serial("Accessibility tests for realm roles", () => {
  const realmName = "role-a11y-" + uuid();
  const defaultRolesMaster = "default-roles-" + realmName;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmRoles(page);
  });

  test("Check a11y violations on load/ realm roles", async ({ page }) => {
    await assertAxeViolations(page);
  });

  test("Check a11y violations on default-roles-master default tab and default roles tabs", async ({
    page,
  }) => {
    await clickTableRowItem(page, defaultRolesMaster);
    await assertAxeViolations(page);

    await page.click("text=Default groups");
    await assertAxeViolations(page);
  });

  test("Check a11y violations on empty create role form", async ({ page }) => {
    await page.click("text=Create role");
    await assertAxeViolations(page);
  });
});
