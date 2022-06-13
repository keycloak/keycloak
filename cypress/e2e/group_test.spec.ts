import ListingPage from "../support/pages/admin_console/ListingPage";
import GroupModal from "../support/pages/admin_console/manage/groups/GroupModal";
import GroupDetailPage from "../support/pages/admin_console/manage/groups/GroupDetailPage";
import AttributesTab from "../support/pages/admin_console/manage//AttributesTab";
import MoveGroupModal from "../support/pages/admin_console/manage/groups/MoveGroupModal";
import { SearchGroupPage } from "../support/pages/admin_console/manage/groups/SearchGroup";
import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import ViewHeaderPage from "../support/pages/ViewHeaderPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import ModalUtils from "../support/util/ModalUtils";

describe("Group test", () => {
  const loginPage = new LoginPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const listingPage = new ListingPage();
  const viewHeaderPage = new ViewHeaderPage();
  const groupModal = new GroupModal();
  const searchGroupPage = new SearchGroupPage();
  const moveGroupModal = new MoveGroupModal();
  const modalUtils = new ModalUtils();
  const attributesTab = new AttributesTab();

  let groupName = "group";

  const clickGroup = (itemName: string) => {
    sidebarPage.waitForPageLoad();
    cy.get("table").contains(itemName).click();

    return this;
  };

  describe("Group creation", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToGroups();
    });

    it("Group CRUD test", () => {
      groupName += "_" + (Math.random() + 1).toString(36).substring(7);

      groupModal
        .open("no-groups-in-this-realm-empty-action")
        .fillGroupForm(groupName)
        .clickCreate();

      masthead.checkNotificationMessage("Group created");

      sidebarPage.goToGroups();
      listingPage.searchItem(groupName, false).itemExist(groupName);

      // Delete
      listingPage.deleteItem(groupName);
      modalUtils.checkModalTitle("Delete group?").confirmModal();
      masthead.checkNotificationMessage("Group deleted");
    });

    it("Should rename group", () => {
      groupModal
        .open("no-groups-in-this-realm-empty-action")
        .fillGroupForm(groupName)
        .clickCreate();
      clickGroup(groupName);
      viewHeaderPage.clickAction("renameGroupAction");

      const newName = "Renamed group";
      groupModal.fillGroupForm(newName).clickRename();
      masthead.checkNotificationMessage("Group updated");

      sidebarPage.goToGroups();
      listingPage.searchItem(newName, false).itemExist(newName);
      listingPage.deleteItem(newName);
      modalUtils.checkModalTitle("Delete group?").confirmModal();
      masthead.checkNotificationMessage("Group deleted");
    });

    it("Should move group", () => {
      const targetGroupName = "target";
      groupModal.open("no-groups-in-this-realm-empty-action");
      groupModal.fillGroupForm(groupName).clickCreate();

      sidebarPage.waitForPageLoad();
      groupModal
        .open("openCreateGroupModal")
        .fillGroupForm(targetGroupName)
        .clickCreate();

      // For some reason, this fixes clickDetailMenu
      sidebarPage.goToEvents();
      sidebarPage.goToGroups();

      listingPage.clickRowDetails(groupName).clickDetailMenu("Move to");
      moveGroupModal
        .clickRow(targetGroupName)
        .checkTitle(`Move ${groupName} to ${targetGroupName}`);
      moveGroupModal.clickMove();

      masthead.checkNotificationMessage("Group moved");
      sidebarPage.waitForPageLoad();
      listingPage.itemExist(groupName, false);
      clickGroup(targetGroupName);
      sidebarPage.waitForPageLoad();
      listingPage.itemExist(groupName);
      sidebarPage.goToGroups();
      listingPage.deleteItem(targetGroupName);
      modalUtils.checkModalTitle("Delete group?").confirmModal();
      masthead.checkNotificationMessage("Group deleted");
    });

    describe("Move groups", () => {
      const groups = ["group", "group1", "group2"];
      before(() => adminClient.createSubGroups(groups));

      after(() => adminClient.deleteGroups());

      it("Should move group to root", () => {
        listingPage.goToItemDetails(groups[0]);
        sidebarPage.waitForPageLoad();
        listingPage.clickRowDetails(groups[1]).clickDetailMenu("Move to");

        moveGroupModal.clickMove();
        sidebarPage.goToGroups();

        new GroupDetailPage().checkListSubGroup(groups.slice(0, -1));
      });

      it("Should move group back", () => {
        listingPage.clickRowDetails(groups[1]).clickDetailMenu("Move to");

        moveGroupModal.clickRow(groups[0]).clickMove();
        sidebarPage.goToGroups();

        new GroupDetailPage().checkListSubGroup(["group"]);
      });
    });

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
      const createdGroups = await adminClient.createSubGroups(groups);
      for (let i = 0; i < 5; i++) {
        const username = "user" + i;
        adminClient.createUserInGroup(username, createdGroups[i % 3].id);
      }
      adminClient.createUser({ username: "new", enabled: true });
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToGroups();
    });

    after(async () => {
      await adminClient.deleteGroups();
      for (let i = 0; i < 5; i++) {
        const username = "user" + i;
        await adminClient.deleteUser(username);
      }
      await adminClient.deleteUser("new");
    });

    it("Should display all the subgroups", () => {
      clickGroup(groups[0]);
      detailPage.checkListSubGroup([groups[1]]);

      const added = "addedGroup";
      groupModal
        .open("openCreateGroupModal")
        .fillGroupForm(added)
        .clickCreate();

      detailPage.checkListSubGroup([added, groups[1]]);
    });

    it("Should display members", () => {
      clickGroup(groups[0]);
      detailPage.clickMembersTab().checkListMembers(["user0", "user3"]);
      detailPage
        .clickIncludeSubGroups()
        .checkListMembers(["user0", "user3", "user1", "user4", "user2"]);
    });

    it("Should add members", () => {
      clickGroup(groups[0]);
      detailPage
        .clickMembersTab()
        .clickAddMembers()
        .checkSelectableMembers(["user1", "user4"]);
      detailPage.selectUsers(["new"]).clickAdd();

      masthead.checkNotificationMessage("1 user added to the group");
      detailPage.checkListMembers(["new", "user0", "user3"]);
    });

    it("Attributes CRUD test", () => {
      clickGroup(groups[0]);
      attributesTab
        .goToAttributesTab()
        .fillLastRow("key", "value")
        .saveAttribute();

      masthead.checkNotificationMessage("Group updated");
    });
  });
});
