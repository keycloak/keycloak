import { v4 as uuid } from "uuid";
import GroupModal from "../support/pages/admin-ui/manage/groups/GroupModal";
import GroupDetailPage from "../support/pages/admin-ui/manage/groups/group_details/GroupDetailPage";
import AttributesTab from "../support/pages/admin-ui/manage/AttributesTab";
import { SearchGroupPage } from "../support/pages/admin-ui/manage/groups/SearchGroupPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import GroupPage from "../support/pages/admin-ui/manage/groups/GroupPage";
import ChildGroupsTab from "../support/pages/admin-ui/manage/groups/group_details/tabs/ChildGroupsTab";
import MembersTab from "../support/pages/admin-ui/manage/groups/group_details/tabs/MembersTab";
import adminClient from "../support/util/AdminClient";
import { range } from "lodash-es";
import RoleMappingTab from "../support/pages/admin-ui/manage/RoleMappingTab";
import CommonPage from "../support/pages/CommonPage";

describe("Group test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const groupModal = new GroupModal();
  const searchGroupPage = new SearchGroupPage();
  const attributesTab = new AttributesTab();
  const groupPage = new GroupPage();
  const groupDetailPage = new GroupDetailPage();
  const childGroupsTab = new ChildGroupsTab();
  const membersTab = new MembersTab();
  const commonPage = new CommonPage();

  const groupNamePrefix = "group_";
  let groupName: string;
  const groupNames: string[] = [];
  const predefinedGroups = ["level", "level1", "level2", "level3"];
  const emptyGroup = "empty-group";
  let users: { id: string; username: string }[] = [];
  const username = "test-user";

  before(async () => {
    users = await Promise.all(
      range(5).map((index) => {
        const user = adminClient
          .createUser({
            username: username + index,
            enabled: true,
          })
          .then((user) => {
            return { id: user.id!, username: username + index };
          });
        return user;
      }),
    );
  });

  after(
    async () =>
      await Promise.all([
        adminClient.deleteGroups(),
        ...range(5).map((index) => adminClient.deleteUser(username + index)),
      ]),
  );

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToGroups();
    groupName = groupNamePrefix + uuid();
    groupNames.push(groupName);
  });

  describe("List", () => {
    it("Create group from empty option", () => {
      groupPage
        .assertNoGroupsInThisRealmEmptyStateMessageExist(true)
        .createGroup(groupName, true)
        .assertNotificationGroupCreated()
        .searchGroup(groupName, true)
        .assertGroupItemExist(groupName, true);
    });

    it("Create group from search bar", () => {
      groupPage
        .assertNoGroupsInThisRealmEmptyStateMessageExist(false)
        .createGroup(groupName, false)
        .assertNotificationGroupCreated()
        .searchGroup(groupName, true)
        .assertGroupItemExist(groupName, true);
    });

    it("Fail to create group with empty name", () => {
      groupPage
        .assertNoGroupsInThisRealmEmptyStateMessageExist(false)
        .createGroup(" ", false)
        .assertNotificationCouldNotCreateGroupWithEmptyName();
      groupModal.closeModal();
    });

    it("Fail to create group with duplicated name", () => {
      groupPage
        .assertNoGroupsInThisRealmEmptyStateMessageExist(false)
        .createGroup(groupName, false)
        .createGroup(groupName, false)
        .assertNotificationCouldNotCreateGroupWithDuplicatedName(groupName);
      groupModal.closeModal();
      groupPage.searchGroup(groupName).assertGroupItemsEqual(1);
    });

    it("Empty search", () => {
      groupPage.searchGroup("   ").assertNoSearchResultsMessageExist(true);
    });

    it("Search group that exists", () => {
      groupPage
        .searchGroup(groupNames[0])
        .assertGroupItemExist(groupNames[0], true);
    });

    it("Search group that does not exists", () => {
      groupPage
        .searchGroup("not-existent-group")
        .assertNoSearchResultsMessageExist(true);
    });

    it("Duplicate group", () => {
      groupPage
        .duplicateGroupItem(groupNames[0], true)
        .assertNotificationGroupDuplicated();
    });

    it("Delete group from item bar", () => {
      groupPage
        .searchGroup(groupNames[0], true)
        .deleteGroupItem(groupNames[0])
        .assertNotificationGroupDeleted()
        .searchGroup(groupNames[0], true)
        .assertNoSearchResultsMessageExist(true);
    });

    it("Delete group from search bar", () => {
      groupPage
        .selectGroupItemCheckbox([groupNames[1]])
        .deleteSelectedGroups()
        .assertNotificationGroupDeleted()
        .searchGroup(groupNames[1])
        .assertNoSearchResultsMessageExist(true);
    });

    it("Delete groups from search bar", () => {
      cy.wrap(null).then(() =>
        adminClient.createGroup("group_multiple_deletion_test"),
      );
      cy.reload();
      groupPage
        .selectGroupItemCheckboxAllRows()
        .deleteSelectedGroups()
        .assertNotificationGroupsDeleted()
        .assertNoGroupsInThisRealmEmptyStateMessageExist(true);
    });
  });

  describe("Search group under current group", () => {
    before(async () => {
      const createdGroups = await adminClient.createSubGroups(predefinedGroups);
      await Promise.all([
        range(5).map((index) => {
          adminClient.addUserToGroup(
            users[index].id!,
            createdGroups[index % 3].id,
          );
        }),
      ]);
    });

    it("Search child group in group", () => {
      groupPage
        .goToGroupChildGroupsTab(predefinedGroups[0])
        .searchGroup(predefinedGroups[1])
        .assertGroupItemExist(predefinedGroups[1], true);
    });

    it("Search non existing child group in group", () => {
      groupPage
        .goToGroupChildGroupsTab(predefinedGroups[0])
        .searchGroup("non-existent-sub-group")
        .assertNoSearchResultsMessageExist(true);
    });

    it("Empty search in group", () => {
      groupPage
        .goToGroupChildGroupsTab(predefinedGroups[0])
        .searchGroup("   ")
        .assertNoSearchResultsMessageExist(true);
    });
  });

  describe("Group Actions", () => {
    const groupNameDeleteHeaderAction = "group_test_delete_header_action";

    before(async () => {
      await adminClient.createGroup(groupNameDeleteHeaderAction);
    });

    after(async () => {
      await adminClient.deleteGroups();
    });

    describe("Search globally", () => {
      it("Navigate to parent group details", () => {
        searchGroupPage
          .searchGroup(predefinedGroups[0])
          .goToGroupChildGroupsTab(predefinedGroups[0])
          .assertGroupItemExist(predefinedGroups[1], true);
      });
    });

    it("Rename group", () => {
      groupPage.goToGroupChildGroupsTab(predefinedGroups[0]);
      groupDetailPage
        .renameGroup("new_group_name")
        .assertNotificationGroupUpdated()
        .assertHeaderGroupNameEqual("new_group_name")
        .renameGroup(predefinedGroups[0])
        .assertNotificationGroupUpdated()
        .assertHeaderGroupNameEqual(predefinedGroups[0]);
    });

    it("Delete group from group details", () => {
      groupPage.goToGroupChildGroupsTab(groupNameDeleteHeaderAction);
      groupDetailPage
        .headerActionDeleteGroup()
        .assertNotificationGroupDeleted()
        .assertGroupItemExist(groupNameDeleteHeaderAction, false);
    });
  });

  describe("Child Groups", () => {
    before(async () => {
      await adminClient.createGroup(predefinedGroups[0]);
    });

    after(async () => {
      await adminClient.deleteGroups();
    });

    beforeEach(() => {
      groupPage.goToGroupChildGroupsTab(predefinedGroups[0]);
    });

    it("Check empty state", () => {
      childGroupsTab.assertNoGroupsInThisSubGroupEmptyStateMessageExist(true);
    });

    it("Create group from empty state", () => {
      childGroupsTab
        .createGroup(predefinedGroups[1], true)
        .assertNotificationGroupCreated();
    });

    it("Create group from search bar", () => {
      childGroupsTab
        .createGroup(predefinedGroups[2], false)
        .assertNotificationGroupCreated();
    });

    it("Fail to create group with empty name", () => {
      childGroupsTab
        .createGroup(" ", false)
        .assertNotificationCouldNotCreateGroupWithEmptyName();
    });

    // https://github.com/keycloak/keycloak-admin-ui/issues/2726
    it.skip("Fail to create group with duplicated name", () => {
      childGroupsTab
        .createGroup(predefinedGroups[2], false)
        .assertNotificationCouldNotCreateGroupWithDuplicatedName(
          predefinedGroups[2],
        );
    });

    it("Move group from item bar", () => {
      childGroupsTab
        .moveGroupItemAction(predefinedGroups[1], [
          predefinedGroups[0],
          predefinedGroups[2],
        ])
        .goToGroupChildGroupsTab(predefinedGroups[2])
        .assertGroupItemExist(predefinedGroups[1], true);
    });

    it("Search group", () => {
      childGroupsTab
        .searchGroup(predefinedGroups[2])
        .assertGroupItemExist(predefinedGroups[2], true);
    });

    it("Show child group in groups", () => {
      childGroupsTab
        .goToGroupChildGroupsTab(predefinedGroups[2])
        .goToGroupChildGroupsTab(predefinedGroups[1])
        .assertNoGroupsInThisSubGroupEmptyStateMessageExist(true);
    });

    it("Delete group from search bar", () => {
      childGroupsTab
        .goToGroupChildGroupsTab(predefinedGroups[2])
        .selectGroupItemCheckbox([predefinedGroups[1]])
        .deleteSelectedGroups()
        .assertNotificationGroupDeleted();
    });

    it("Delete group from item bar", () => {
      childGroupsTab
        .deleteGroupItem(predefinedGroups[2])
        .assertNotificationGroupDeleted()
        .assertNoGroupsInThisSubGroupEmptyStateMessageExist(true);
    });
  });

  describe("Members", () => {
    before(async () => {
      const createdGroups = await adminClient.createSubGroups(predefinedGroups);
      await Promise.all([
        range(5).map((index) => {
          adminClient.addUserToGroup(
            users[index].id!,
            createdGroups[index % 3].id,
          );
        }),
        adminClient.createGroup(emptyGroup),
        adminClient.createUser({ username: "new", enabled: true }),
      ]);
    });

    after(() => adminClient.deleteUser("new"));

    beforeEach(() => {
      groupPage.goToGroupChildGroupsTab(predefinedGroups[0]);
      childGroupsTab.goToMembersTab();
    });

    it("Add member from search bar", () => {
      membersTab
        .addMember(["new"], false)
        .assertNotificationUserAddedToTheGroup(1);
    });

    it("Show members with sub-group users", () => {
      membersTab
        .assertUserItemExist(users[0].username, true)
        .assertUserItemExist("new", true)
        .assertUserItemExist(users[3].username, true)
        .clickCheckboxIncludeSubGroupUsers()
        .assertUserItemExist("new", true)
        .assertUserItemExist(users[0].username, true)
        .assertUserItemExist(users[1].username, true)
        .assertUserItemExist(users[2].username, true)
        .assertUserItemExist(users[3].username, true)
        .assertUserItemExist(users[4].username, true)
        .goToChildGroupsTab()
        .goToGroupChildGroupsTab(predefinedGroups[1])
        .goToMembersTab()
        .assertUserItemExist(users[1].username, true)
        .assertUserItemExist(users[4].username, true)
        .goToChildGroupsTab()
        .goToGroupChildGroupsTab(predefinedGroups[2])
        .goToMembersTab()
        .assertUserItemExist(users[2].username, true);
    });

    it("Add member from empty state", () => {
      sidebarPage.goToGroups();
      groupPage.goToGroupChildGroupsTab(emptyGroup);
      childGroupsTab.goToMembersTab();
      membersTab
        .addMember([users[0].username, users[1].username], true)
        .assertNotificationUserAddedToTheGroup(2);
    });

    it("Leave group from search bar", () => {
      sidebarPage.goToGroups();
      groupPage.goToGroupChildGroupsTab(emptyGroup);
      childGroupsTab.goToMembersTab();
      membersTab
        .selectUserItemCheckbox([users[0].username])
        .leaveGroupSelectedUsers()
        .assertNotificationUserLeftTheGroup(1)
        .assertUserItemExist(users[0].username, false);
    });

    it("Leave group from item bar", () => {
      sidebarPage.goToGroups();
      groupPage.goToGroupChildGroupsTab(emptyGroup);
      childGroupsTab.goToMembersTab();
      membersTab
        .leaveGroupUserItem(users[1].username)
        .assertNotificationUserLeftTheGroup(1)
        .assertNoUsersFoundEmptyStateMessageExist(true);
    });

    it("Show memberships from item bar", () => {
      sidebarPage.goToGroups();
      groupPage.goToGroupChildGroupsTab(predefinedGroups[0]);
      childGroupsTab.goToMembersTab();
      membersTab
        .showGroupMembershipsItem(users[3].username)
        .assertGroupItemExist(predefinedGroups[0], true)
        .cancelShowGroupMembershipsModal();
    });
  });

  describe("Breadcrumbs", () => {
    it("Navigate to parent group", () => {
      groupPage
        .goToGroupChildGroupsTab(predefinedGroups[0])
        .goToGroupChildGroupsTab(predefinedGroups[1])
        .goToGroupChildGroupsTab(predefinedGroups[2])
        .goToGroupChildGroupsTab(predefinedGroups[3]);
      cy.reload();
      groupPage.clickBreadcrumbItem(predefinedGroups[2]);
      groupDetailPage.assertHeaderGroupNameEqual(predefinedGroups[2]);
      groupPage.clickBreadcrumbItem(predefinedGroups[1]);
      groupDetailPage.assertHeaderGroupNameEqual(predefinedGroups[1]);
      groupPage.clickBreadcrumbItem(predefinedGroups[0]);
      groupDetailPage.assertHeaderGroupNameEqual(predefinedGroups[0]);
      groupPage
        .clickBreadcrumbItem("Groups")
        .assertGroupItemExist(predefinedGroups[0], true);
    });
  });

  describe("Attributes", () => {
    beforeEach(() => {
      groupPage.goToGroupChildGroupsTab(predefinedGroups[0]);
      groupDetailPage.goToAttributesTab();
    });

    it("Add attribute", () => {
      attributesTab.addAttribute("key", "value").save();
      groupPage.assertNotificationGroupUpdated();
    });

    it("Remove attribute", () => {
      attributesTab.deleteAttribute(0);
      attributesTab.assertEmpty();
      groupPage.assertNotificationGroupUpdated();
    });

    it("Revert changes", () => {
      attributesTab
        .addAttribute("key", "value")
        .addAnAttributeButton()
        .revert()
        .assertEmpty();
    });
  });

  describe("'Move to' function", () => {
    it("Move group to other group", () => {
      groupPage
        .moveGroupItemAction(predefinedGroups[0], [emptyGroup])
        .goToGroupChildGroupsTab(emptyGroup)
        .assertGroupItemExist(predefinedGroups[0], true);
    });

    it("Move group to root", () => {
      groupPage
        .goToGroupChildGroupsTab(emptyGroup)
        .moveGroupItemAction(predefinedGroups[0], ["root"]);
      sidebarPage.goToGroups();
      groupPage.assertGroupItemExist(predefinedGroups[0], true);
    });
  });

  describe("Role mappings", () => {
    const roleMappingTab = new RoleMappingTab("group");

    beforeEach(() => {
      groupPage.goToGroupChildGroupsTab(predefinedGroups[0]);
      groupDetailPage.goToRoleMappingTab();
    });

    it("Check empty state", () => {
      commonPage.emptyState().checkIfExists(true);
    });

    it("Assign roles from empty state", () => {
      roleMappingTab.assignRole();
      roleMappingTab
        .changeRoleTypeFilter("roles")
        .selectRow("default-roles-")
        .assign();
    });

    it("Show and search roles", () => {
      groupDetailPage.checkDefaultRole();
    });

    it("Check hide inherited roles option", () => {
      roleMappingTab.unhideInheritedRoles();
      roleMappingTab.hideInheritedRoles();
    });

    it("Remove roles", () => {
      roleMappingTab.selectRow("default-roles");
      roleMappingTab.unAssign();
      groupDetailPage.deleteRole();
    });
  });

  describe("Permissions", () => {
    beforeEach(() => {
      groupPage.goToGroupChildGroupsTab(predefinedGroups[0]);
      groupDetailPage.goToPermissionsTab();
    });

    it("enable/disable permissions", () => {
      groupDetailPage.enablePermissionSwitch();
    });
  });

  describe("Accessibility tests for groups", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToGroups();
      cy.injectAxe();
    });

    it("Check a11y violations on load/ groups", () => {
      cy.checkA11y();
    });
  });
});
