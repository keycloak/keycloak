import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../../utils/AdminClient.ts";
import { login } from "../../utils/login.ts";
import { assertNotificationMessage } from "../../utils/masthead.ts";
import { cancelModal } from "../../utils/modal.ts";
import { goToOrganizations, goToRealm } from "../../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  clickSelectRow,
  clickTableRowItem,
} from "../../utils/table.ts";
import {
  addMember,
  assertIncludeSubGroupUsersNotVisible,
  goToMembersTab,
  leaveGroup,
} from "../../groups/members.ts";
import { goToChildGroupsTab } from "../../groups/util.ts";
import { goToOrgGroupsTab } from "../groups.ts";

test.describe.serial("Org Group Members", () => {
  const realmName = `org-groups-members-${uuid()}`;
  const orgName = `org-${uuid()}`;
  const predefinedGroups = ["level", "level1", "level2", "level3"];
  const emptyGroup = "empty-group";
  const users: { id: string; username: string }[] = [];
  const username = "test-user";
  const tableName = "Members";

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createOrganization({
      realm: realmName,
      name: orgName,
      domains: [{ name: `${orgName}.org`, verified: false }],
    });

    const createdGroups = await adminClient.createOrgSubGroups(
      orgName,
      predefinedGroups,
      realmName,
    );

    for (let index = 0; index < 5; index++) {
      const user = await adminClient.createUser({
        username: username + index,
        enabled: true,
        realm: realmName,
      });
      users.push({ id: user.id!, username: username + index });
      await adminClient.addOrgMember(orgName, user.id!, realmName);
      await adminClient.addUserToOrgGroup(
        user.id!,
        createdGroups[index % 3].id,
        orgName,
        realmName,
      );
    }

    await adminClient.createOrgGroup(orgName, emptyGroup, realmName);

    const newUser = await adminClient.createUser({
      username: "new",
      enabled: true,
      realm: realmName,
    });
    await adminClient.addOrgMember(orgName, newUser.id!, realmName);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, orgName);
    await goToOrgGroupsTab(page);
    await clickTableRowItem(page, predefinedGroups[0]);
    await goToMembersTab(page);
  });

  test("Add member from search bar", async ({ page }) => {
    await addMember(page, ["new"], false);
    await assertNotificationMessage(page, "1 user added to the group");
  });

  // The "Include sub-group users" feature is not supported for org groups (#47055).
  // Verify the checkbox is not shown.
  test("Include sub-group users checkbox is not shown for org groups", async ({
    page,
  }) => {
    await assertIncludeSubGroupUsersNotVisible(page);
  });

  test("Add member from empty state", async ({ page }) => {
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, orgName);
    await goToOrgGroupsTab(page);
    await clickTableRowItem(page, emptyGroup);
    await goToMembersTab(page);
    await addMember(page, [users[0].username, users[1].username], true);
    await assertNotificationMessage(page, "2 users added to the group");
  });

  test("Leave group from search bar", async ({ page }) => {
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, orgName);
    await goToOrgGroupsTab(page);
    await clickTableRowItem(page, predefinedGroups[0]);
    await goToMembersTab(page);
    await clickSelectRow(page, tableName, users[0].username);
    await leaveGroup(page);
    await assertNotificationMessage(page, "1 user left the group");
    await assertRowExists(page, users[0].username, false);
  });

  test("Leave group from item bar", async ({ page }) => {
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, orgName);
    await goToOrgGroupsTab(page);
    await clickTableRowItem(page, predefinedGroups[0]);
    await goToMembersTab(page);
    await clickRowKebabItem(page, users[3].username, "Leave");
    await assertNotificationMessage(page, "1 user left the group");
    await assertRowExists(page, users[3].username, false);
  });

  test("Show memberships from item bar", async ({ page }) => {
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, orgName);
    await goToOrgGroupsTab(page);
    await clickTableRowItem(page, predefinedGroups[0]);
    await goToChildGroupsTab(page);
    await clickTableRowItem(page, predefinedGroups[1]);
    await goToMembersTab(page);
    await clickRowKebabItem(page, users[1].username, "Show memberships");
    await assertRowExists(page, predefinedGroups[0], true);
    await cancelModal(page);
  });
});
