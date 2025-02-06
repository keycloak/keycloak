import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../../cypress/support/util/AdminClient";
import { clickSaveButton, switchOn } from "../utils/form";
import { login } from "../utils/login";
import { assertAxeViolations } from "../utils/masthead";
import { goToRealm, goToRealmRoles } from "../utils/sidebar";
import { clickTableRowItem } from "../utils/table";

test.describe("Accessibility tests for realm roles", () => {
  const realmName = "role-a11y-" + uuid();
  const role = "a11y-role-" + uuid();
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

  test("Check a11y violations on role details", async ({ page }) => {
    await page.click("text=Create role");
    await page.fill("input[name='name']", role);
    await clickSaveButton(page);
    await assertAxeViolations(page);

    await page.click("text=Attributes");
    await assertAxeViolations(page);

    await page.click("text=Users in role");
    await assertAxeViolations(page);

    await page.click("text=Permissions");
    await switchOn(page, "#permissionsEnabled");
    await assertAxeViolations(page);
  });
});
