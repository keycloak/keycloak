import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../../utils/AdminClient.ts";
import { login } from "../../utils/login.ts";
import {
  assertAxeViolations,
  assertNotificationMessage,
} from "../../utils/masthead.ts";
import { cancelModal, confirmModal } from "../../utils/modal.ts";
import { goToOrganizations, goToRealm } from "../../utils/sidebar.ts";
import {
  assertNoResults,
  assertRowExists,
  clickRowKebabItem,
  clickSelectRow,
  clickTableRowItem,
  clickTableToolbarItem,
  searchItem,
} from "../../utils/table.ts";
import { editGroup, searchGroup } from "../../groups/list.ts";
import { goToGroupDetails } from "../../groups/util.ts";
import {
  createOrgGroup,
  goToOrgGroupsTab,
  selectOrgGroupActionToggleItem,
} from "../groups.ts";

const realmName = `org-groups-list-${uuid()}`;
const orgName = `org-${uuid()}`;

async function navigateToOrgGroups(page: Parameters<typeof login>[0]) {
  await login(page);
  await goToRealm(page, realmName);
  await goToOrganizations(page);
  await clickTableRowItem(page, orgName);
  await goToOrgGroupsTab(page);
}

test.describe.serial("Org group list tests", () => {
  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createOrganization({
      realm: realmName,
      name: orgName,
      domains: [{ name: `${orgName}.org`, verified: false }],
    });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.describe.serial("Org group CRUD", () => {
    const groupName = `group-${uuid()}`;

    test.beforeEach(async ({ page }) => {
      await navigateToOrgGroups(page);
    });

    test("Create group from empty state", async ({ page }) => {
      await createOrgGroup(page, groupName, "", true);
      await assertNotificationMessage(page, "Group created");
      await searchGroup(page, groupName);
      await assertRowExists(page, groupName, true);
    });

    test("Create group from toolbar", async ({ page }) => {
      const secondGroupName = `group-second-${uuid()}`;
      await createOrgGroup(
        page,
        secondGroupName,
        "some sort of description",
        false,
      );
      await assertNotificationMessage(page, "Group created");
      await clickTableRowItem(page, secondGroupName);
      await expect(page.getByText("some sort of description")).toBeVisible();
      await page.goBack();

      await searchGroup(page, secondGroupName);
      await assertRowExists(page, secondGroupName, true);
      await adminClient.deleteOrgGroups(orgName, realmName);
    });

    test("Fail to create group with empty name", async ({ page }) => {
      // List is empty after deleteOrgGroups in the previous test, so use empty state
      await createOrgGroup(page, " ", "", true);
      await assertNotificationMessage(
        page,
        "Could not create group Group name is missing",
      );
    });

    test("Fail to create group with duplicated name", async ({ page }) => {
      // List is still empty — create via empty state, then try duplicate via toolbar
      await createOrgGroup(page, groupName, "", true);
      await createOrgGroup(page, groupName, "", false);
      await assertNotificationMessage(
        page,
        "Could not create group Group with the given name already exists.",
      );
      await cancelModal(page);
    });
  });

  test.describe.serial("Search and manage org groups", () => {
    const predefinedGroups = ["level", "level1", "level2", "level3"];
    const placeholder = "Filter groups";
    const tableName = "Groups";

    test.beforeEach(async ({ page }) => {
      for (const group of predefinedGroups) {
        await adminClient.createOrgGroup(orgName, group, realmName);
      }
      await navigateToOrgGroups(page);
    });

    test.afterEach(() => adminClient.deleteOrgGroups(orgName, realmName));

    test("Search group that exists", async ({ page }) => {
      await searchItem(page, placeholder, predefinedGroups[1]);
      await assertRowExists(page, predefinedGroups[1]);
    });

    test("Search group that does not exist", async ({ page }) => {
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

    test("Delete group from toolbar", async ({ page }) => {
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
      await selectOrgGroupActionToggleItem(page, "Delete group");
      await confirmModal(page);
      await assertNotificationMessage(page, "Group deleted");
      await assertRowExists(page, predefinedGroups[2], false);
    });

    test("Check a11y violations on org groups page", async ({ page }) => {
      await assertAxeViolations(page);
    });
  });
});
