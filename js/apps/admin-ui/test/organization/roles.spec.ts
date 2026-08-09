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
  const secondMemberName = "alternate-organization-role-user";

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
    const secondMember = await adminClient.createUser({
      realm: realmName,
      username: secondMemberName,
      enabled: true,
    });
    await adminClient.addOrgMember(
      organizationName,
      secondMember.id!,
      realmName,
    );
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

  test("manages roles, composites, and paginated members", async ({ page }) => {
    const parent = roleName("team-lead");
    const child = roleName("team-member");
    const nested = roleName("effective-nested");

    try {
      await createRole(page, parent);
      await createRole(page, child);
      await createRole(page, nested);

      await openRole(page, child);
      await addComposite(page, nested);
      await returnToRoleList(page);

      await openRole(page, parent);
      await page.locator('textarea[name="description"]').fill("Leads the team");
      await clickSaveButton(page);
      await assertNotificationMessage(page, "Organization role saved");
      await expect(page.locator('textarea[name="description"]')).toHaveValue(
        "Leads the team",
      );

      await addComposite(page, child);
      await assertRowExists(page, nested, false);

      await page.getByTestId("hideInheritedRoles").click();
      await assertRowExists(page, nested);
      await removeComposite(page, child);
      await assertRowExists(page, child, false);

      await page.getByTestId("organization-role-users-tab").click();
      await page.getByRole("button", { name: "Assign members" }).click();

      const candidateSearch = page.getByPlaceholder("Search user");
      await candidateSearch.fill(secondMemberName);
      await candidateSearch.press("Enter");
      await assertRowExists(page, secondMemberName);
      await assertRowExists(page, memberName, false);
      await clickSelectRow(page, "Users", secondMemberName);

      await candidateSearch.fill("");
      await candidateSearch.press("Enter");
      await clickSelectRow(page, "Users", memberName);
      await page.getByRole("button", { name: "Assign", exact: true }).click();
      await assertNotificationMessage(
        page,
        "2 members assigned to the organization role",
      );
      await assertRowExists(page, memberName);
      await assertRowExists(page, secondMemberName);
      await expect(page.getByRole("row", { name: memberName })).toBeVisible();

      const search = page.getByPlaceholder("Search members");
      await search.fill(secondMemberName);
      await search.press("Enter");
      await assertRowExists(page, secondMemberName);
      await assertRowExists(page, memberName, false);

      await search.fill("");
      await search.press("Enter");
      await assertRowExists(page, memberName);
      await assertRowExists(page, secondMemberName);

      await clickRowKebabItem(page, memberName, "Remove");
      await confirmModal(page);
      await assertNotificationMessage(
        page,
        "1 member removed from the organization role",
      );
      await assertRowExists(page, memberName, false);
      await expect(page.getByRole("row", { name: memberName })).toBeHidden();

      await returnToRoleList(page);
      await deleteRoleFromList(page, parent);
    } finally {
      await cleanupRole(page, parent);
      await cleanupRole(page, child);
      await cleanupRole(page, nested);
    }
  });

  test("protects the default role and deletes regular roles", async ({
    page,
  }) => {
    const child = roleName("default-child");
    const regularRole = roleName("delete-me");
    const defaultRole = `default-roles-org-${organizationAlias}`;

    try {
      await createRole(page, child);
      await createRole(page, regularRole);
      await deleteRoleFromList(page, regularRole);
      await expect(page.getByRole("row", { name: regularRole })).toBeHidden();

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
      await cleanupRole(page, regularRole);
    }
  });
});
