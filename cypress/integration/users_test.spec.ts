import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import CreateUserPage from "../support/pages/admin_console/manage/users/CreateUserPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import UserDetailsPage from "../support/pages/admin_console/manage/users/UserDetailsPage";
import AttributesTab from "../support/pages/admin_console/manage/AttributesTab";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_before";
import GroupModal from "../support/pages/admin_console/manage/groups/GroupModal";
import UserGroupsPage from "../support/pages/admin_console/manage/users/UserGroupsPage";
import AdminClient from "../support/util/AdminClient";

let groupName = "group";
let groupsList: string[] = [];

describe("Group creation", () => {
  const loginPage = new LoginPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const groupModal = new GroupModal();

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToGroups();
  });

  function createNewGroup() {
    groupName += "_" + (Math.random() + 1).toString(36).substring(7);

    sidebarPage.waitForPageLoad();
    groupModal.open().fillGroupForm(groupName).clickCreate();

    groupsList = [...groupsList, groupName];
    masthead.checkNotificationMessage("Group created");

    sidebarPage.goToGroups();
  }

  it("Add groups to be joined", () => {
    for (let i = 0; i <= 2; i++) {
      createNewGroup();
    }
  });
});

describe("Users test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const createUserPage = new CreateUserPage();
  const userGroupsPage = new UserGroupsPage();
  const masthead = new Masthead();
  const modalUtils = new ModalUtils();
  const listingPage = new ListingPage();
  const userDetailsPage = new UserDetailsPage();
  const attributesTab = new AttributesTab();

  let itemId = "user_crud";

  describe("User creation", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToUsers();
    });

    after(async () => {
      await new AdminClient().deleteGroups();
    });

    it("Go to create User page", () => {
      createUserPage.goToCreateUser();
      cy.url().should("include", "users/add-user");

      // Verify Cancel button works
      createUserPage.cancel();
      cy.url().should("not.include", "/add-user");
    });

    it("Create user test", () => {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);

      // Create
      createUserPage.goToCreateUser();

      createUserPage.createUser(itemId);

      createUserPage.toggleAddGroupModal();

      const groupsListCopy = groupsList.slice(0, 1);

      console.log(groupsList);

      groupsListCopy.forEach((element) => {
        cy.findByTestId(`${element}-check`).click();
      });

      createUserPage.joinGroups();

      createUserPage.save();

      masthead.checkNotificationMessage("The user has been created");

      sidebarPage.goToUsers();
    });

    it("User details test", () => {
      listingPage.searchItem(itemId).itemExist(itemId);

      listingPage.goToItemDetails(itemId);

      userDetailsPage.fillUserData().save();

      masthead.checkNotificationMessage("The user has been saved");

      sidebarPage.goToUsers();
      listingPage.searchItem(itemId).itemExist(itemId);
    });

    it("User attributes test", () => {
      listingPage.searchItem(itemId).itemExist(itemId);

      listingPage.goToItemDetails(itemId);

      attributesTab
        .goToAttributesTab()
        .fillAttribute("key", "value")
        .saveAttribute();

      masthead.checkNotificationMessage("The user has been saved");
    });

    it("Add user to groups test", () => {
      // Go to user groups
      listingPage.searchItem(itemId).itemExist(itemId);
      listingPage.goToItemDetails(itemId);

      userGroupsPage.goToGroupsTab();
      userGroupsPage.toggleAddGroupModal();

      const groupsListCopy = groupsList.slice(1, 2);

      groupsListCopy.forEach((element) => {
        cy.findByTestId(`${element}-check`).click();
      });

      userGroupsPage.joinGroups();
    });

    it("Leave group test", () => {
      listingPage.searchItem(itemId).itemExist(itemId);
      listingPage.goToItemDetails(itemId);
      // Go to user groups
      userGroupsPage.goToGroupsTab();
      cy.contains("Leave").click();
      cy.findByTestId("modalConfirm").click();
    });

    it("Go to user consents test", () => {
      listingPage.searchItem(itemId).itemExist(itemId);

      listingPage.goToItemDetails(itemId);

      cy.findByTestId("user-consents-tab").click();
      cy.findByTestId("empty-state").contains("No consents");
    });

    it("Delete user test", () => {
      // Delete
      listingPage.deleteItem(itemId);

      modalUtils.checkModalTitle("Delete user?").confirmModal();

      masthead.checkNotificationMessage("The user has been deleted");

      listingPage.itemExist(itemId, false);
    });
  });
});
