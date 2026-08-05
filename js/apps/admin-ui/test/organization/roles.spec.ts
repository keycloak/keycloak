import { expect, type Page, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToOrganizations, goToRealm } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  clickSelectRow,
  clickTableRowItem,
} from "../utils/table.ts";

test.describe.serial("Organization roles", () => {
  const realmName = `organization-roles-${uuid()}`;
  const organizationName = "role-organization";
  const organizationAlias = "role-org";
  const memberName = "organization-role-member";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createOrganization({
      realm: realmName,
      name: organizationName,
      alias: organizationAlias,
      domains: [{ name: "roles.example", verified: false }],
    });
    const member = await adminClient.createUser({
      realm: realmName,
      username: memberName,
      enabled: true,
    });
    await adminClient.addOrgMember(organizationName, member.id!, realmName);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToOrganizationRoles(page);
  });

  const roleName = (prefix: string) => `${prefix}-${uuid()}`;

  const goToOrganizationRoles = async (page: Page) => {
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, organizationName);
    await page.getByTestId("rolesTab").click();
  };

  const createRole = async (page: Page, name: string) => {
    await page.getByTestId("create-organization-role").click();
    await page.locator('input[name="name"]').fill(name);
    await page.getByRole("button", { name: "Create", exact: true }).click();
    await assertNotificationMessage(page, "Organization role created");
    await assertRowExists(page, name);
  };

  const openRole = async (page: Page, name: string) => {
    await clickTableRowItem(page, name);
  };

  const returnToRoleList = async (page: Page) => {
    await page.getByTestId("organization-role-details-tab").click();
    await page.getByTestId("cancel").click();
  };

  const addComposite = async (page: Page, name: string) => {
    await page.getByTestId("organization-role-associated-roles-tab").click();
    await page.getByRole("button", { name: "Add associated roles" }).click();
    await clickSelectRow(page, "Available roles", name);
    await page.getByTestId("assign-organization-role-composites").click();
    await assertNotificationMessage(page, "Associated roles added");
    await assertRowExists(page, name);
  };

  const removeComposite = async (page: Page, name: string) => {
    await clickSelectRow(page, "Associated roles", name);
    await page.getByRole("button", { name: "Unassign" }).click();
    await confirmModal(page);
    await assertNotificationMessage(page, "Associated roles removed");
    await assertRowExists(page, name, false);
  };

  const deleteRoleFromList = async (page: Page, name: string) => {
    await clickRowKebabItem(page, name, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, "The role has been deleted");
    await assertRowExists(page, name, false);
  };

  const cleanupRole = async (page: Page, name: string) => {
    await goToOrganizationRoles(page);
    const row = page.getByRole("row", { name });
    if (await row.isVisible().catch(() => false)) {
      await clickRowKebabItem(page, name, "Delete");
      await confirmModal(page);
      await row.waitFor({ state: "hidden", timeout: 5000 }).catch(() => {
        return undefined;
      });
    }
  };

  test("creates and edits an organization role", async ({ page }) => {
    const role = roleName("team-lead");

    try {
      await createRole(page, role);
      await openRole(page, role);
      await page.locator('textarea[name="description"]').fill("Leads the team");
      await clickSaveButton(page);
      await assertNotificationMessage(page, "Organization role saved");
      await expect(page.locator('textarea[name="description"]')).toHaveValue(
        "Leads the team",
      );
    } finally {
      await cleanupRole(page, role);
    }
  });

  test("assigns and removes organization role composites", async ({ page }) => {
    const parent = roleName("team-lead");
    const child = roleName("team-member");

    try {
      await createRole(page, parent);
      await createRole(page, child);
      await openRole(page, parent);
      await addComposite(page, child);
      await expect(page.getByRole("row", { name: child })).toBeVisible();
      await removeComposite(page, child);
      await expect(page.getByRole("row", { name: child })).toBeHidden();
    } finally {
      await cleanupRole(page, parent);
      await cleanupRole(page, child);
    }
  });

  test("shows effective organization role composites", async ({ page }) => {
    const parent = roleName("effective-parent");
    const child = roleName("effective-child");
    const nested = roleName("effective-nested");

    try {
      await createRole(page, parent);
      await createRole(page, child);
      await createRole(page, nested);

      await openRole(page, child);
      await addComposite(page, nested);
      await returnToRoleList(page);

      await openRole(page, parent);
      await addComposite(page, child);
      await assertRowExists(page, nested, false);

      await page.getByTestId("hideInheritedRoles").click();
      await assertRowExists(page, nested);
      await expect(page.getByRole("row", { name: nested })).toBeVisible();
    } finally {
      await cleanupRole(page, parent);
      await cleanupRole(page, child);
      await cleanupRole(page, nested);
    }
  });

  test("assigns and removes organization role members", async ({ page }) => {
    const role = roleName("team-member");

    try {
      await createRole(page, role);
      await openRole(page, role);
      await page.getByTestId("organization-role-users-tab").click();
      await page.getByRole("button", { name: "Assign members" }).click();
      await clickSelectRow(page, "Users", memberName);
      await page.getByRole("button", { name: "Assign", exact: true }).click();
      await assertNotificationMessage(
        page,
        "1 member assigned to the organization role",
      );
      await assertRowExists(page, memberName);
      await expect(page.getByRole("row", { name: memberName })).toBeVisible();

      await clickRowKebabItem(page, memberName, "Remove");
      await confirmModal(page);
      await assertNotificationMessage(
        page,
        "1 member removed from the organization role",
      );
      await assertRowExists(page, memberName, false);
      await expect(page.getByRole("row", { name: memberName })).toBeHidden();
    } finally {
      await cleanupRole(page, role);
    }
  });

  test("protects the default organization role", async ({ page }) => {
    const child = roleName("default-child");
    const defaultRole = `default-roles-${organizationAlias}`;

    try {
      await createRole(page, child);
      await assertRowExists(page, defaultRole);
      await openRole(page, defaultRole);
      await expect(page.getByTestId("delete-organization-role")).toBeHidden();
      await expect(
        page.getByTestId("organization-role-users-tab"),
      ).toBeHidden();

      await addComposite(page, child);
      await removeComposite(page, child);
    } finally {
      await cleanupRole(page, child);
    }
  });

  test("deletes a non-default organization role", async ({ page }) => {
    const role = roleName("delete-me");

    await createRole(page, role);
    await deleteRoleFromList(page, role);
    await expect(page.getByRole("row", { name: role })).toBeHidden();
  });
});
