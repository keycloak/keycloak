import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import CreateUserPage from "../support/pages/admin_console/manage/users/CreateUserPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import UserDetailsPage from "../support/pages/admin_console/manage/users/UserDetailsPage";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_before";
import GroupModal from "../support/pages/admin_console/manage/groups/GroupModal";
import UserGroupsPage from "../support/pages/admin_console/manage/users/UserGroupsPage";

let groupName = "group";

describe("Group creation", () => {
  const loginPage = new LoginPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const listingPage = new ListingPage();
  const groupModal = new GroupModal();

  beforeEach(function () {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToGroups();
  });

  it("Add group to be joined", () => {
    groupName += "_" + (Math.random() + 1).toString(36).substring(7);

    groupModal
      .open("openCreateGroupModal")
      .fillGroupForm(groupName)
      .clickCreate();

    masthead.checkNotificationMessage("Group created");

    sidebarPage.goToGroups();
    listingPage.searchItem(groupName, false).itemExist(groupName);
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

  let itemId = "user_crud";

  describe("User creation", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToUsers();
    });

    it("Go to create User page", () => {
      cy.wait(100);

      createUserPage.goToCreateUser();
      cy.url().should("include", "users/add-user");

      // Verify Cancel button works
      createUserPage.cancel();
      cy.url().should("not.include", "/add-user");
    });

    it("Create user test", function () {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);

      // Create
      cy.wait(100);

      createUserPage.goToCreateUser();

      createUserPage.createUser(itemId).save();

      masthead.checkNotificationMessage("The user has been created");

      sidebarPage.goToUsers();
    });

    it("User details test", function () {
      cy.wait(1000);
      listingPage.searchItem(itemId).itemExist(itemId);

      cy.wait(1000);
      listingPage.goToItemDetails(itemId);

      userDetailsPage.fillUserData().save();

      masthead.checkNotificationMessage("The user has been saved");

      cy.wait(1000);

      sidebarPage.goToUsers();
      listingPage.searchItem(itemId).itemExist(itemId);
    });

    it("Add user to group test", function () {
      // Go to user groups

      listingPage.searchItem(itemId).itemExist(itemId);
      listingPage.goToItemDetails(itemId);

      userGroupsPage.goToGroupsTab();
      userGroupsPage.toggleAddGroupModal();
      cy.getId(`${groupName}`).click();
      userGroupsPage.joinGroup();

      cy.wait(1000);

      listingPage.itemExist(groupName);
    });

    it("Leave group test", function () {
      listingPage.searchItem(itemId).itemExist(itemId);
      listingPage.goToItemDetails(itemId);
      // Go to user groups
      userGroupsPage.goToGroupsTab();
      cy.getId(`leave-${groupName}`).click();
      cy.getId("modalConfirm").click();
    });

    it("Go to user consents test", function () {
      cy.wait(1000);
      listingPage.searchItem(itemId).itemExist(itemId);

      cy.wait(1000);
      listingPage.goToItemDetails(itemId);

      cy.getId("user-consents-tab").click();

      cy.getId("empty-state").contains("No consents");
    });

    it("Delete user test", function () {
      // Delete
      cy.wait(1000);
      listingPage.deleteItem(itemId);

      modalUtils.checkModalTitle("Delete user?").confirmModal();

      masthead.checkNotificationMessage("The user has been deleted");

      listingPage.itemExist(itemId, false);
    });
  });
});