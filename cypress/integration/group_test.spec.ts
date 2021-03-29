import ListingPage from "../support/pages/admin_console/ListingPage";
import GroupModal from "../support/pages/admin_console/manage/groups/GroupModal";
import GroupDetailPage from "../support/pages/admin_console/manage/groups/GroupDetailPage";
import MoveGroupModal from "../support/pages/admin_console/manage/groups/MoveGroupModal";
import { SearchGroupPage } from "../support/pages/admin_console/manage/groups/SearchGroup";
import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import ViewHeaderPage from "../support/pages/ViewHeaderPage";
import AdminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_before";

describe("Group test", () => {
  const loginPage = new LoginPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const listingPage = new ListingPage();
  const viewHeaderPage = new ViewHeaderPage();
  const groupModal = new GroupModal();
  const searchGroupPage = new SearchGroupPage();
  const moveGroupModal = new MoveGroupModal();

  let groupName = "group";

  describe("Group creation", () => {
    beforeEach(function () {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToGroups();
    });

    it("Group CRUD test", () => {
      groupName += "_" + (Math.random() + 1).toString(36).substring(7);

      groupModal
        .open("empty-primary-action")
        .fillGroupForm(groupName)
        .clickCreate();

      masthead.checkNotificationMessage("Group created");

      sidebarPage.goToGroups();
      listingPage.searchItem(groupName, false).itemExist(groupName);

      // Delete
      listingPage.deleteItem(groupName);
      masthead.checkNotificationMessage("Group deleted");
    });

    it("Should rename group", () => {
      groupModal
        .open("empty-primary-action")
        .fillGroupForm(groupName)
        .clickCreate();
      listingPage.goToItemDetails(groupName);
      viewHeaderPage.clickAction("renameGroupAction");

      const newName = "Renamed group";
      groupModal.fillGroupForm(newName).clickRename();
      masthead.checkNotificationMessage("Group updated");

      sidebarPage.goToGroups();
      listingPage.searchItem(newName, false).itemExist(newName);
      listingPage.deleteItem(newName);
    });

    it("Group search", () => {
      viewHeaderPage.clickAction("searchGroup");
      searchGroupPage.searchGroup("group").clickSearchButton();
      searchGroupPage.checkTerm("group");
    });

    it("Should move group", () => {
      const targetGroupName = "target";
      groupModal
        .open("empty-primary-action")
        .fillGroupForm(groupName)
        .clickCreate();

      groupModal.open().fillGroupForm(targetGroupName).clickCreate();

      listingPage.clickRowDetails(groupName).clickDetailMenu("Move to");
      moveGroupModal
        .clickRow(targetGroupName)
        .checkTitle(`Move ${groupName} to ${targetGroupName}`);
      moveGroupModal.clickMove();

      masthead.checkNotificationMessage("Group moved");
      listingPage
        .itemExist(groupName, false)
        .goToItemDetails(targetGroupName)
        .itemExist(targetGroupName);

      sidebarPage.goToGroups();
      listingPage.deleteItem(targetGroupName);
    });
  });

  describe("Group details", () => {
    const groups = ["level", "level1", "level2"];
    const detailPage = new GroupDetailPage();

    before(async () => {
      const client = new AdminClient();
      const createdGroups = await client.createSubGroups(groups);
      for (let i = 0; i < 5; i++) {
        const username = "user" + i;
        client.createUserInGroup(username, createdGroups[i % 3].id);
      }
      client.createUser({ username: "new", enabled: true });
    });

    beforeEach(() => {
      cy.visit("");
      loginPage.logIn();
      sidebarPage.goToGroups();
    });

    after(() => {
      new AdminClient().deleteGroups();
    });

    it("Should display all the subgroups", () => {
      listingPage.goToItemDetails(groups[0]);
      detailPage.checkListSubGroup([groups[1]]);

      const added = "addedGroup";
      groupModal.open().fillGroupForm(added).clickCreate();

      detailPage.checkListSubGroup([added, groups[1]]);
    });

    it("Should display members", () => {
      listingPage.goToItemDetails(groups[0]);
      detailPage.clickMembersTab().checkListMembers(["user0", "user3"]);
      detailPage
        .clickIncludeSubGroups()
        .checkListMembers(["user0", "user3", "user1", "user4", "user2"]);
    });

    it("Should add members", () => {
      listingPage.goToItemDetails(groups[0]);
      detailPage
        .clickMembersTab()
        .clickAddMembers()
        .checkSelectableMembers(["user1", "user4"]);
      detailPage.selectUsers(["new"]).clickAdd();

      masthead.checkNotificationMessage("1 user added to the group");
      detailPage.checkListMembers(["new", "user0", "user3"]);
    });

    it("Attributes CRUD test", () => {
      listingPage.goToItemDetails(groups[0]);
      detailPage
        .clickAttributesTab()
        .fillAttribute("key", "value")
        .saveAttribute();

      masthead.checkNotificationMessage("Group updated");
    });
  });
});
