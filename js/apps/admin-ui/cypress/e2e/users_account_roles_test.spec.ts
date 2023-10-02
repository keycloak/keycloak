import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import CreateUserPage from "../support/pages/admin-ui/manage/users/CreateUserPage";
import CredentialsPage from "../support/pages/admin-ui/manage/users/CredentialsPage";
import RoleMappingTab from "../support/pages/admin-ui/manage/RoleMappingTab";
import CreateProviderPage from "../support/pages/admin-ui/manage/identity_providers/CreateProviderPage";
import RequiredActions from "../support/pages/admin-ui/manage/authentication/RequiredActions";
import adminClient from "../support/util/AdminClient";
import ModalUtils from "../support/util/ModalUtils";
import ListingPage from "../support/pages/admin-ui/ListingPage";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const createUserPage = new CreateUserPage();
const credentialsPage = new CredentialsPage();
const roleMappingTab = new RoleMappingTab("");
const createProviderPage = new CreateProviderPage();
const requiredActionsPage = new RequiredActions();
const modalUtils = new ModalUtils();
const listingPage = new ListingPage();
const itemId = "test";

describe("User account roles tests", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToUsers();

    createUserPage.goToCreateUser();
    createUserPage.createUser(itemId);
    createUserPage.save();
    masthead.checkNotificationMessage("The user has been created");
    sidebarPage.waitForPageLoad();

    credentialsPage
      .goToCredentialsTab()
      .clickEmptyStatePasswordBtn()
      .fillPasswordFormWithTempOff()
      .clickConfirmationBtn()
      .clickSetPasswordBtn();
  });

  afterEach(() => {
    adminClient.deleteUser(itemId);
  });

  it("should check that user with inherited roles (view-profile, manage-account-links, manage-account) can access and perform specific actions in account console", () => {
    const identityProviderName = "bitbucket";
    const deletePrompt = "Delete provider?";
    const deleteSuccessMsg = "Provider successfully deleted.";

    sidebarPage.goToIdentityProviders();
    createProviderPage
      .clickItem("bitbucket-card")
      .fill(identityProviderName, "123")
      .clickAdd();

    masthead.signOut();
    loginPage.logIn("test", "test");
    keycloakBefore();
    masthead.accountManagement();
    cy.visit("http://localhost:8180/realms/master/account/");

    //Check that user can view personal info
    cy.findByTestId("username").should("have.value", "test");

    //Check that user can update email in personal info
    cy.findByTestId("email").type("test@test.com");
    cy.findByTestId("firstName").type("testFirstName");
    cy.findByTestId("lastName").type("testLastName");
    cy.findByTestId("save").click();
    cy.get(
      'h4.pf-c-alert__title:contains("Your account has been updated.")',
    ).should("exist");

    //Check that user doesn't have access to delete account from personal info
    cy.contains("Delete account").should("not.exist");

    //Check that user can access linked accounts under account security
    cy.contains("Account security").click();
    cy.contains("Linked accounts").click();
    cy.contains("Link account").should("exist");

    //Check that user doesn't have access to groups
    cy.contains("Groups").should("not.exist");

    //Clean up
    masthead.signOutFromAccount();
    loginPage.logIn("admin", "admin");
    keycloakBefore();

    sidebarPage.goToIdentityProviders();
    listingPage.deleteItem(identityProviderName);
    modalUtils.checkModalTitle(deletePrompt).confirmModal();
    masthead.checkNotificationMessage(deleteSuccessMsg, true);
  });

  it("should check that user with delete-account role has an access to delete account in account console", () => {
    roleMappingTab.goToRoleMappingTab();
    roleMappingTab.addClientRole("delete-account");
    roleMappingTab.selectRow("delete-account", true).assign();

    sidebarPage.goToAuthentication();

    const action = "Delete Account";
    requiredActionsPage.enableAction(action);
    masthead.checkNotificationMessage("Updated required action successfully");
    requiredActionsPage.isChecked(action);

    masthead.signOut();
    loginPage.logIn("test", "test");
    keycloakBefore();
    masthead.accountManagement();
    cy.visit("http://localhost:8180/realms/master/account/");

    //Check that user has access to delete account from personal info
    cy.contains("Delete account").should("exist");

    //Cleanup
    masthead.signOutFromAccount();
    loginPage.logIn("admin", "admin");
    keycloakBefore();

    sidebarPage.goToAuthentication();
    requiredActionsPage.enableAction(action);
    masthead.checkNotificationMessage("Updated required action successfully");
    requiredActionsPage.isChecked(action);
  });

  it("should check that user with view-groups role has an access to groups in account console", () => {
    roleMappingTab.goToRoleMappingTab();
    roleMappingTab.addClientRole("view-groups");
    roleMappingTab.selectRow("view-groups", true).assign();

    masthead.signOut();
    loginPage.logIn("test", "test");
    keycloakBefore();
    masthead.accountManagement();
    cy.visit("http://localhost:8180/realms/master/account/");

    //Check that user has access to view groups page
    cy.contains("Groups").should("exist").click();
    cy.get('h1.pf-c-title:contains("Groups")').should("exist");
  });
});
