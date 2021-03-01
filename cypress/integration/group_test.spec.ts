import ListingPage from "../support/pages/admin_console/ListingPage";
import CreateGroupModal from "../support/pages/admin_console/manage/groups/CreateGroupModal";
import { SearchGroupPage } from "../support/pages/admin_console/manage/groups/SearchGroup";
import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import ViewHeaderPage from "../support/pages/ViewHeaderPage";

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
      listingPage.searchItem(groupName).itemExist(groupName);

      // Delete
      listingPage.deleteItem(groupName);
      masthead.checkNotificationMessage("Group deleted");

      listingPage.itemExist(groupName, false);
    });

    const searchGroupPage = new SearchGroupPage();
    it("Group search", () => {
      viewHeaderPage.clickAction("searchGroup");
      searchGroupPage.searchGroup("group").clickSearchButton();
      searchGroupPage.checkTerm("group");
    });
  });
});
