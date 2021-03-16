import ListingPage from "../support/pages/admin_console/ListingPage";
import CreateGroupModal from "../support/pages/admin_console/manage/groups/CreateGroupModal";
import GroupDetailPage from "../support/pages/admin_console/manage/groups/GroupDetailPage";
import { SearchGroupPage } from "../support/pages/admin_console/manage/groups/SearchGroup";
import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import ViewHeaderPage from "../support/pages/ViewHeaderPage";
import AdminClient from "../support/util/AdminClient";

describe("Group test", () => {
  const loginPage = new LoginPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const listingPage = new ListingPage();
  const viewHeaderPage = new ViewHeaderPage();
  const createGroupModal = new CreateGroupModal();

  let groupName = "group";

  describe("Group creation", () => {
    beforeEach(function () {
      cy.visit("");
      loginPage.logIn();
      sidebarPage.goToGroups();
    });

    it("Group CRUD test", () => {
      groupName += "_" + (Math.random() + 1).toString(36).substring(7);

      createGroupModal
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

    const searchGroupPage = new SearchGroupPage();
    it("Group search", () => {
      viewHeaderPage.clickAction("searchGroup");
      searchGroupPage.searchGroup("group").clickSearchButton();
      searchGroupPage.checkTerm("group");
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
      createGroupModal.open().fillGroupForm(added).clickCreate();

      detailPage.checkListSubGroup([added, groups[1]]);
    });

    it("Should display members", () => {
      listingPage.goToItemDetails(groups[0]);
      detailPage.clickMembersTab().checkListMembers(["user0", "user3"]);
      detailPage
        .clickIncludeSubGroups()
        .checkListMembers(["user0", "user3", "user1", "user4", "user2"]);
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
