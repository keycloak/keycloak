import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import CreateUserPage from "../support/pages/admin_console/manage/users/CreateUserPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import UserDetailsPage from "../support/pages/admin_console/manage/users/UserDetailsPage";
import AttributesTab from "../support/pages/admin_console/manage/AttributesTab";
import ModalUtils from "../support/util/ModalUtils";
import {
  keycloakBefore,
  keycloakBeforeEach,
} from "../support/util/keycloak_hooks";
import UserGroupsPage from "../support/pages/admin_console/manage/users/UserGroupsPage";
import AdminClient from "../support/util/AdminClient";
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
  let itemIdWithCred = "user_crud_cred";
  const adminClient = new AdminClient();

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
    keycloakBeforeEach();
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

    attributesTab
      .goToAttributesTab()
      .fillLastRow("key", "value")
      .saveAttribute();

    masthead.checkNotificationMessage("The user has been saved");
  });

  it("User attributes with multiple values test", () => {
    listingPage.searchItem(itemId).itemExist(itemId);
    listingPage.goToItemDetails(itemId);

    cy.intercept("PUT", `/auth/admin/realms/master/users/*`).as("save-user");

    const attributeKey = "key-multiple";
    attributesTab
      .goToAttributesTab()
      .fillLastRow(attributeKey, "other value")
      .saveAttribute();

    cy.wait("@save-user").should(({ request, response }) => {
      expect(response?.statusCode).to.equal(204);

      expect(request?.body.attributes, "response body").deep.equal({
        key: ["value"],
        "key-multiple": ["other value"],
      });
    });

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
    cy.findByTestId(`leave-${groupsList[0]}`).click();
    cy.findByTestId("confirm").click();
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

  // TODO: Fix this test so it passes.
  it.skip("Delete user test", () => {
    // Delete
    listingPage.deleteItem(itemId);

    modalUtils.checkModalTitle("Delete user?").confirmModal();

    masthead.checkNotificationMessage("The user has been deleted");
    sidebarPage.waitForPageLoad();

    listingPage.itemExist(itemId, false);
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
