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
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, organizationName);
    await page.getByTestId("rolesTab").click();
  });

  const createRole = async (page: Page, name: string) => {
    await page.getByTestId("create-organization-role").click();
    await page.locator('input[name="name"]').fill(name);
    await page.getByRole("button", { name: "Create", exact: true }).click();
    await assertNotificationMessage(page, "Organization role created");
    await assertRowExists(page, name);
  };

  test("manages roles, composites, members, and the default role", async ({
    page,
  }) => {
    const parentRole = "team-lead";
    const childRole = "team-member";
    const defaultRole = `default-roles-${organizationAlias}`;

    await assertRowExists(page, defaultRole);
    await createRole(page, parentRole);
    await createRole(page, childRole);

    await clickTableRowItem(page, parentRole);
    await page.locator('textarea[name="description"]').fill("Leads the team");
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Organization role saved");

    await page.getByTestId("organization-role-associated-roles-tab").click();
    await page.getByRole("button", { name: "Add associated roles" }).click();
    await clickSelectRow(page, "Available roles", childRole);
    await page.getByTestId("assign-organization-role-composites").click();
    await assertNotificationMessage(page, "Associated roles added");
    await assertRowExists(page, childRole);

    await page.getByTestId("organization-role-users-tab").click();
    await page.getByRole("button", { name: "Assign members" }).click();
    await clickSelectRow(page, "Users", memberName);
    await page.getByRole("button", { name: "Assign", exact: true }).click();
    await assertNotificationMessage(
      page,
      "1 member assigned to the organization role",
    );
    await assertRowExists(page, memberName);

    await clickRowKebabItem(page, memberName, "Remove");
    await confirmModal(page);
    await assertNotificationMessage(
      page,
      "1 member removed from the organization role",
    );

    await page.getByTestId("organization-role-details-tab").click();
    await page.getByTestId("cancel").click();
    await clickTableRowItem(page, defaultRole);
    await expect(page.getByTestId("delete-organization-role")).toBeHidden();
    await page.getByTestId("organization-role-associated-roles-tab").click();
    await page.getByRole("button", { name: "Add associated roles" }).click();
    await clickSelectRow(page, "Available roles", childRole);
    await page.getByTestId("assign-organization-role-composites").click();
    await assertNotificationMessage(page, "Associated roles added");

    await page.getByTestId("organization-role-details-tab").click();
    await page.getByTestId("cancel").click();
    await clickRowKebabItem(page, parentRole, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, "The role has been deleted");
    await assertRowExists(page, parentRole, false);
  });
});
