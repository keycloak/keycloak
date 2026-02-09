import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import {
  assertAxeViolations,
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { cancelModal, confirmModal } from "../utils/modal.ts";
import { goToGroups } from "../utils/sidebar.ts";
import {
  assertNoResults,
  assertRowExists,
  clickRowKebabItem,
  clickSelectRow,
  clickTableRowItem,
  clickTableToolbarItem,
  searchItem,
} from "../utils/table.ts";
import { createGroup, editGroup, searchGroup } from "./list.ts";
import { goToGroupDetails } from "./util.ts";

test.describe.serial("Group test", () => {
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
    await createGroup(page, groupName, "", true);
    await assertNotificationMessage(page, "Group created");
    await searchGroup(page, groupName);
    await assertRowExists(page, groupName, true);

    // create group from search bar
    const secondGroupName = `group-second-${uuid()}`;
    await createGroup(page, secondGroupName, "some sort of description", false);
    await assertNotificationMessage(page, "Group created");
    await clickTableRowItem(page, secondGroupName);
    await expect(page.getByText("some sort of description")).toBeVisible();
    await page.goBack();

    await searchGroup(page, secondGroupName);
    await assertRowExists(page, secondGroupName, true);
    await adminClient.deleteGroups();
  });

  test("Fail to create group with empty name", async ({ page }) => {
    await createGroup(page, " ", "", true);
    await assertNotificationMessage(
      page,
      "Could not create group Group name is missing",
    );
  });

  test("Fail to create group with duplicated name", async ({ page }) => {
    await createGroup(page, groupName, "", true);
    await createGroup(page, groupName, "", false);
    await assertNotificationMessage(
      page,
      `Could not create group Top level group named '${groupName}' already exists.`,
    );
    await cancelModal(page);
  });
});

test.describe.serial("Search group under current group", () => {
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

  test("Edit group", async ({ page }) => {
    const newGroupName = "new_group_name";
    const description = "new description";
    await clickRowKebabItem(page, predefinedGroups[3], "Edit");
    await editGroup(page, newGroupName, description);
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
