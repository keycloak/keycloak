import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import CreateUserPage from "../support/pages/admin_console/manage/users/CreateUserPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import UserDetailsPage from "../support/pages/admin_console/manage/users/UserDetailsPage";
import AttributesTab from "../support/pages/admin_console/manage/AttributesTab";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import UserGroupsPage from "../support/pages/admin_console/manage/users/UserGroupsPage";
import adminClient from "../support/util/AdminClient";
import CredentialsPage from "../support/pages/admin_console/manage/users/CredentialsPage";

let groupName = "group";
let groupsList: string[] = [];

describe("User creation", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const createUserPage = new CreateUserPage();
  const userGroupsPage = new UserGroupsPage();
  const masthead = new Masthead();
  const modalUtils = new ModalUtils();
  const listingPage = new ListingPage();
  const userDetailsPage = new UserDetailsPage();
  const credentialsPage = new CredentialsPage();
  const attributesTab = new AttributesTab();

  let itemId = "user_crud";
  let itemIdWithGroups = "user_with_groups_crud";
  let itemIdWithCred = "user_crud_cred";
  const itemCredential = "Password";

  before(() => {
    for (let i = 0; i <= 2; i++) {
      groupName += "_" + (Math.random() + 1).toString(36).substring(7);
      adminClient.createGroup(groupName);
      groupsList = [...groupsList, groupName];
    }

    keycloakBefore();
    loginPage.logIn();
  });

  beforeEach(() => {
    sidebarPage.goToUsers();
  });

  after(() => {
    adminClient.deleteGroups();
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

    createUserPage.save();

    masthead.checkNotificationMessage("The user has been created");
  });

  it("Create user with groups test", () => {
    itemIdWithGroups += (Math.random() + 1).toString(36).substring(7);
    // Add user from search bar
    createUserPage.goToCreateUser();

    createUserPage.createUser(itemIdWithGroups);

    createUserPage.toggleAddGroupModal();

    const groupsListCopy = groupsList.slice(0, 1);

    groupsListCopy.forEach((element) => {
      cy.findByTestId(`${element}-check`).click();
    });

    createUserPage.joinGroups();

    createUserPage.save();

    masthead.checkNotificationMessage("The user has been created");
  });

  it("Create user with credentials test", () => {
    itemIdWithCred += "_" + (Math.random() + 1).toString(36).substring(7);

    // Add user from search bar
    createUserPage.goToCreateUser();

    createUserPage.createUser(itemIdWithCred);

    userDetailsPage.fillUserData();
    createUserPage.save();
    masthead.checkNotificationMessage("The user has been created");
    sidebarPage.waitForPageLoad();

    credentialsPage
      .goToCredentialsTab()
      .clickEmptyStatePasswordBtn()
      .fillPasswordForm()
      .clickConfirmationBtn()
      .clickSetPasswordBtn();
  });

  it("Search existing user test", () => {
    listingPage.searchItem(itemId).itemExist(itemId);
  });

  it("Search non-existing user test", () => {
    listingPage.searchItem("user_DNE");
    cy.findByTestId(listingPage.emptyState).should("exist");
  });

  it("User details test", () => {
    sidebarPage.waitForPageLoad();
    listingPage.searchItem(itemId).itemExist(itemId);

    listingPage.goToItemDetails(itemId);

    userDetailsPage.fillUserData().save();

    masthead.checkNotificationMessage("The user has been saved");

    sidebarPage.waitForPageLoad();
    sidebarPage.goToUsers();
    listingPage.searchItem(itemId).itemExist(itemId);
  });

  it("User attributes test", () => {
    listingPage.goToItemDetails(itemId);

    attributesTab.goToAttributesTab().addAttribute("key", "value").save();

    masthead.checkNotificationMessage("The user has been saved");
  });

  it("User attributes with multiple values test", () => {
    listingPage.searchItem(itemId).itemExist(itemId);
    listingPage.goToItemDetails(itemId);

    cy.intercept("PUT", `/admin/realms/master/users/*`).as("save-user");

    const attributeKey = "key-multiple";
    attributesTab
      .goToAttributesTab()
      .addAttribute(attributeKey, "other value")
      .save();

    cy.wait("@save-user").should(({ request, response }) => {
      expect(response?.statusCode).to.equal(204);

      expect(request.body.attributes, "response body").deep.equal({
        key: ["value"],
        "key-multiple": ["other value"],
      });
    });

    masthead.checkNotificationMessage("The user has been saved");
  });

  it("Add user to groups test", () => {
    masthead.closeAllAlertMessages();
    // Go to user groups
    listingPage.searchItem(itemId).itemExist(itemId);
    listingPage.goToItemDetails(itemId);

    userGroupsPage.goToGroupsTab();
    userGroupsPage.toggleAddGroupModal();

    const groupsListCopy = groupsList.slice(0, 1);

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
    cy.findByTestId(`leave-${groupsList[0]}`).click();
    cy.findByTestId("confirm").click({ force: true });
  });

  it("Go to user consents test", () => {
    listingPage.searchItem(itemId).itemExist(itemId);

    sidebarPage.waitForPageLoad();
    listingPage.goToItemDetails(itemId);

    cy.findByTestId("user-consents-tab").click();
    cy.findByTestId("empty-state").contains("No consents");
  });

  it("Reset credential of User with empty state", () => {
    listingPage.goToItemDetails(itemId);
    credentialsPage
      .goToCredentialsTab()
      .clickEmptyStateResetBtn()
      .fillResetCredentialForm();
    masthead.checkNotificationMessage(
      "Failed: Failed to send execute actions email"
    );
  });

  it("Reset credential of User with existing credentials", () => {
    listingPage.goToItemDetails(itemIdWithCred);
    credentialsPage
      .goToCredentialsTab()
      .clickResetBtn()
      .fillResetCredentialForm();

    masthead.checkNotificationMessage(
      "Failed: Failed to send execute actions email"
    );
  });

  it("Edit credential label", () => {
    listingPage.goToItemDetails(itemIdWithCred);
    credentialsPage
      .goToCredentialsTab()
      .clickEditCredentialLabelBtn()
      .fillEditCredentialForm()
      .clickEditConfirmationBtn();

    masthead.checkNotificationMessage(
      "The user label has been changed successfully."
    );
  });

  it("Show credential data dialog", () => {
    listingPage.goToItemDetails(itemIdWithCred);
    credentialsPage
      .goToCredentialsTab()
      .clickShowDataDialogBtn()
      .clickCloseDataDialogBtn();
  });

  it("Delete credential", () => {
    listingPage.goToItemDetails(itemIdWithCred);
    credentialsPage.goToCredentialsTab();
    masthead.closeAllAlertMessages();
    cy.wait(2000);
    listingPage.deleteItem(itemCredential);

    modalUtils.checkModalTitle("Delete credentials?").confirmModal();

    masthead.checkNotificationMessage(
      "The credentials has been deleted successfully."
    );
  });

  it("Delete user from search bar test", () => {
    // Delete
    sidebarPage.waitForPageLoad();

    listingPage.searchItem(itemId).itemExist(itemId);
    listingPage.deleteItemFromSearchBar(itemId);

    modalUtils.checkModalTitle("Delete user?").confirmModal();

    masthead.checkNotificationMessage("The user has been deleted");
    sidebarPage.waitForPageLoad();

    listingPage.itemExist(itemId, false);
  });

  it("Delete user with groups test", () => {
    // Delete
    listingPage.deleteItem(itemIdWithGroups);

    modalUtils.checkModalTitle("Delete user?").confirmModal();

    masthead.checkNotificationMessage("The user has been deleted");
    sidebarPage.waitForPageLoad();

    listingPage.itemExist(itemIdWithGroups, false);
  });

  it("Delete user with credential test", () => {
    // Delete
    listingPage.deleteItem(itemIdWithCred);

    modalUtils.checkModalTitle("Delete user?").confirmModal();

    masthead.checkNotificationMessage("The user has been deleted");
    sidebarPage.waitForPageLoad();

    listingPage.itemExist(itemIdWithCred, false);
  });
});
