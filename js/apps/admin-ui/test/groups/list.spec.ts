import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { login } from "../utils/login";
import {
  assertAxeViolations,
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead";
import { cancelModal, confirmModal } from "../utils/modal";
import { goToGroups } from "../utils/sidebar";
import {
  assertNoResults,
  assertRowExists,
  clickRowKebabItem,
  clickSelectRow,
  clickTableToolbarItem,
  searchItem,
} from "../utils/table";
import { createGroup, renameGroup, searchGroup } from "./list";
import { goToGroupDetails } from "./util";

test.describe("Group test", () => {
  const groupName = `group-${uuid()}`;
  const users: { id: string; username: string }[] = [];
  const username = "test-user";

  test.beforeAll(async () => {
    for (let i = 0; i < 5; i++) {
      const user = await adminClient.createUser({
        username: username + i,
        enabled: true,
      });
      users.push({ id: user.id!, username: username + i });
    }
  });

  test.afterAll(async () => {
    await adminClient.deleteGroups();
    for (let i = 0; i < 5; i++) {
      await adminClient.deleteUser(username + i);
    }
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToGroups(page);
  });

  test("Create group test", async ({ page }) => {
    await createGroup(page, groupName, true);
    await assertNotificationMessage(page, "Group created");
    await searchGroup(page, groupName);
    await assertRowExists(page, groupName, true);

    // create group from search bar
    const secondGroupName = `group-second-${uuid()}`;
    await createGroup(page, secondGroupName, false);
    await assertNotificationMessage(page, "Group created");

    await searchGroup(page, secondGroupName);
    await assertRowExists(page, secondGroupName, true);
    await adminClient.deleteGroups();
  });

  test("Fail to create group with empty name", async ({ page }) => {
    await createGroup(page, " ", true);
    await assertNotificationMessage(
      page,
      "Could not create group Group name is missing",
    );
  });

  test("Fail to create group with duplicated name", async ({ page }) => {
    await createGroup(page, groupName, true);
    await createGroup(page, groupName, false);
    await assertNotificationMessage(
      page,
      `Could not create group Top level group named '${groupName}' already exists.`,
    );
    await cancelModal(page);
  });
});

test.describe("Search group under current group", () => {
  const predefinedGroups = ["level", "level1", "level2", "level3"];

  const placeholder = "Filter groups";
  const tableName = "Groups";

  test.beforeEach(async ({ page }) => {
    for (const group of predefinedGroups) {
      await adminClient.createGroup(group);
    }
    await login(page);
    await goToGroups(page);
  });

  test.afterEach(() => adminClient.deleteGroups());

  test("Search group that exists", async ({ page }) => {
    await searchItem(page, placeholder, predefinedGroups[1]);
    await assertRowExists(page, predefinedGroups[1]);
  });

  test("Search group that does not exists", async ({ page }) => {
    await searchItem(page, placeholder, "not-existent-group");
    await assertNoResults(page);
  });

  test("Duplicate group from item bar", async ({ page }) => {
    await clickRowKebabItem(page, predefinedGroups[1], "Duplicate");
    await page.getByTestId("duplicateGroup").click();
    await assertNotificationMessage(page, "Group duplicated");
    await assertRowExists(page, `Copy of ${predefinedGroups[1]}`, true);
  });

  test("Delete group from item bar", async ({ page }) => {
    await clickRowKebabItem(page, predefinedGroups[1], "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, "Group deleted");
    await assertRowExists(page, predefinedGroups[1], false);
  });

  test("Delete group from search bar", async ({ page }) => {
    await clickSelectRow(page, tableName, predefinedGroups[2]);
    await clickTableToolbarItem(page, "Delete", true);
    await confirmModal(page);
    await assertNotificationMessage(page, "Group deleted");
    await assertRowExists(page, predefinedGroups[2], false);
  });

  test("Rename group", async ({ page }) => {
    const newGroupName = "new_group_name";
    await clickRowKebabItem(page, predefinedGroups[3], "Rename");
    await renameGroup(page, newGroupName);
    await assertNotificationMessage(page, "Group updated");
    await assertRowExists(page, newGroupName);
    await assertRowExists(page, predefinedGroups[3], false);
  });

  test("Delete group from group details", async ({ page }) => {
    await goToGroupDetails(page, predefinedGroups[2]);
    await selectActionToggleItem(page, "Delete group");
    await confirmModal(page);
    await assertNotificationMessage(page, "Group deleted");
    await assertRowExists(page, predefinedGroups[2], false);
  });

  test("Check a11y violations on groups page", async ({ page }) => {
    await assertAxeViolations(page);
  });
});
