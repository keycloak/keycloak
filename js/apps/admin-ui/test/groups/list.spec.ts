import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import { createTestBed, type TestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import {
  assertAxeViolations,
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { cancelModal, confirmModal } from "../utils/modal.ts";
import { goToGroups, goToRealm } from "../utils/sidebar.ts";
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
  let testBed: TestBed;
  const users: { id: string; username: string }[] = [];
  const usernamePrefix = `test-user-${uuid()}-`;

  test.beforeAll(async () => {
    testBed = await createTestBed();
    for (let i = 0; i < 5; i++) {
      const username = `${usernamePrefix}${i}`;
      const user = await adminClient.createUser({
        username,
        enabled: true,
        realm: testBed.realm,
      });
      users.push({ id: user.id!, username });
    }
  });

  test.afterAll(async () => {
    await adminClient.deleteGroups(testBed.realm);
    for (const { username } of users) {
      await adminClient.deleteUser(username, testBed.realm, true);
    }
    await testBed[Symbol.asyncDispose]();
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, testBed.realm);
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
    await adminClient.deleteGroups(testBed.realm);
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
  const groupSuffix = uuid();
  const predefinedGroups = ["level", "level1", "level2", "level3"].map(
    (group) => `${group}-${groupSuffix}`,
  );
  let testBed: TestBed;

  const placeholder = "Filter groups";
  const tableName = "Groups";

  test.beforeAll(async () => {
    testBed = await createTestBed();
  });

  test.beforeEach(async ({ page }) => {
    for (const group of predefinedGroups) {
      await adminClient.createGroup(group, testBed.realm);
    }
    await login(page);
    await goToRealm(page, testBed.realm);
    await goToGroups(page);
  });

  test.afterEach(async () => {
    await adminClient.deleteGroups(testBed.realm);
  });

  test.afterAll(async () => {
    await testBed[Symbol.asyncDispose]();
  });

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
    const newGroupName = `new-group-name-${groupSuffix}`;
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
