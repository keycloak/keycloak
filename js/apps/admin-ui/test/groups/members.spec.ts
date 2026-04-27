import { test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { cancelModal } from "../utils/modal.ts";
import { goToGroups } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  clickSelectRow,
  clickTableRowItem,
} from "../utils/table.ts";
import {
  addMember,
  goToMembersTab,
  leaveGroup,
  toggleIncludeSubGroupUsers,
} from "./members.ts";
import { goToChildGroupsTab } from "./util.ts";

test.describe.serial("Members", () => {
  const predefinedGroups = ["level", "level1", "level2", "level3"];
  const emptyGroup = "empty-group";
  const users: { id: string; username: string }[] = [];
  const username = "test-user";
  const tableName = "Members";

  test.beforeAll(async () => {
    const createdGroups = await adminClient.createSubGroups(predefinedGroups);
    for (let index = 0; index < 5; index++) {
      const { id } = await adminClient.createUser({
        username: username + index,
        enabled: true,
      });
      users.push({ id: id!, username: username + index });
    }

    for (let index = 0; index < 5; index++) {
      await adminClient.addUserToGroup(
        users[index].id!,
        createdGroups[index % 3].id,
      );
    }
    await adminClient.createGroup(emptyGroup);
    await adminClient.createUser({
      username: "new",
      enabled: true,
    });
  });

  test.afterAll(async () => {
    for (const user of users) {
      await adminClient.deleteUser(user.username);
    }
    await adminClient.deleteUser("new");
    await adminClient.deleteGroups();
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToGroups(page);
    await clickTableRowItem(page, predefinedGroups[0]);
    await goToMembersTab(page);
  });

  test("Add member from search bar", async ({ page }) => {
    await addMember(page, ["new"], false);
    await assertNotificationMessage(page, "1 user added to the group");
  });

  test("Show members with sub-group users", async ({ page }) => {
    // Check initial users
    await assertRowExists(page, users[0].username, true);
    await assertRowExists(page, users[3].username, true);

    // Toggle sub-group users and check additional users
    await toggleIncludeSubGroupUsers(page);

    for (const user of users) {
      await assertRowExists(page, user.username, true);
    }

    // Navigate to child groups and check users
    await goToChildGroupsTab(page);
    await clickTableRowItem(page, predefinedGroups[1]);
    await goToMembersTab(page);
    await assertRowExists(page, users[1].username, true);
    await assertRowExists(page, users[4].username, true);

    await goToChildGroupsTab(page);
    await clickTableRowItem(page, predefinedGroups[2]);
    await goToMembersTab(page);
    await assertRowExists(page, users[2].username, true);
  });

  test("Add member from empty state", async ({ page }) => {
    await goToGroups(page);
    await clickTableRowItem(page, emptyGroup);
    await goToMembersTab(page);
    await addMember(page, [users[0].username, users[1].username], true);
    await assertNotificationMessage(page, "2 users added to the group");
  });

  test("Leave group from search bar", async ({ page }) => {
    await goToGroups(page);
    await clickTableRowItem(page, predefinedGroups[0]);
    await goToMembersTab(page);
    await clickSelectRow(page, tableName, users[0].username);
    await leaveGroup(page);
    await assertNotificationMessage(page, "1 user left the group");
    await assertRowExists(page, users[0].username, false);
  });

  test("Leave group from item bar", async ({ page }) => {
    await goToGroups(page);
    await clickTableRowItem(page, predefinedGroups[0]);
    await goToMembersTab(page);
    await clickRowKebabItem(page, users[3].username, "Leave");
    await assertNotificationMessage(page, "1 user left the group");
    await assertRowExists(page, users[3].username, false);
  });

  test("Show memberships from item bar", async ({ page }) => {
    await goToGroups(page);
    await clickTableRowItem(page, predefinedGroups[0]);
    await goToChildGroupsTab(page);
    await clickTableRowItem(page, predefinedGroups[1]);

    await goToMembersTab(page);
    await clickRowKebabItem(page, users[1].username, "Show memberships");
    await assertRowExists(page, predefinedGroups[0], true);
    await cancelModal(page);
  });
});
