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
import AdminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_before";
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

    // This test doesn't seem to do anything.  The "Move to" dialog never opens.
    // But the test somehow still passes.
    it("Should move group to root", async () => {
      const groups = ["group1", "group2"];
      groupModal
        .open("no-groups-in-this-realm-empty-action")
        .fillGroupForm(groups[0])
        .clickCreate();
      groupModal
        .open("openCreateGroupModal")
        .fillGroupForm(groups[1])
        .clickCreate();
      listingPage.clickRowDetails(groups[0]).clickDetailMenu("Move to");

      moveGroupModal.clickRoot().clickMove();
      sidebarPage.goToGroups();

      new GroupDetailPage().checkListSubGroup(groups);
      listingPage.deleteItem(groups[0]);
      modalUtils.checkModalTitle("Delete group?").confirmModal();
      listingPage.deleteItem(groups[1]);
      modalUtils.checkModalTitle("Delete group?").confirmModal();
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
      const client = new AdminClient();
      const createdGroups = await client.createSubGroups(groups);
      for (let i = 0; i < 5; i++) {
        const username = "user" + i;
        client.createUserInGroup(username, createdGroups[i % 3].id);
      }
      client.createUser({ username: "new", enabled: true });
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToGroups();
    });

    after(async () => {
      const adminClient = new AdminClient();
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
        .fillAttribute("key", "value")
        .saveAttribute();

      masthead.checkNotificationMessage("Group updated");
    });
  });
});
