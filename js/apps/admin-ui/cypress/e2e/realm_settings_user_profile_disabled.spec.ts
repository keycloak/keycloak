import { v4 as uuid } from "uuid";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import CreateUserPage from "../support/pages/admin-ui/manage/users/CreateUserPage";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const realmSettingsPage = new RealmSettingsPage();
const createUserPage = new CreateUserPage();
const masthead = new Masthead();
const listingPage = new ListingPage();

describe("User profile disabled", () => {
  const realmName = "Realm_" + uuid();

  before(async () => {
    await adminClient.createRealm(realmName, {
      attributes: { userProfileEnabled: "false" },
    });
  });

  after(async () => {
    await adminClient.deleteRealm(realmName);
  });

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealm(realmName);
  });

  afterEach(() => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToLoginTab();
    cy.wait(1000);
    cy.findByTestId("email-as-username-switch").uncheck({ force: true });
    cy.findByTestId("edit-username-switch").uncheck({ force: true });
  });

  it("Create user with email as username, edit username and user profile disabled", () => {
    // Ensure email as username and edit username are disabled
    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToLoginTab();
    cy.findByTestId("email-as-username-switch").should("have.value", "off");
    cy.findByTestId("edit-username-switch").should("have.value", "off");

    // Create user
    sidebarPage.goToUsers();
    createUserPage.goToCreateUser();
    createUserPage.createUser("testuser1");
    createUserPage.save();
    masthead.checkNotificationMessage("The user has been created");

    // Check that username is readonly
    cy.get("#kc-username").should("have.attr", "readonly");
  });

  it("Create user with email as username enabled, edit username disabled and user profile disabled", () => {
    // Create user and check that username is not visible and that email is editable
    sidebarPage.goToUsers();
    createUserPage.goToCreateUser();
    createUserPage.createUser("testuser2");
    createUserPage.save();
    masthead.checkNotificationMessage("The user has been created");

    // Ensure email as username is enabled and edit username are disabled
    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToLoginTab();
    cy.findByTestId("email-as-username-switch").check({ force: true });
    cy.findByTestId("email-as-username-switch").should("have.value", "on");
    cy.findByTestId("edit-username-switch").should("have.value", "off");

    //Find user and do some checks
    sidebarPage.goToUsers();
    sidebarPage.waitForPageLoad();
    listingPage.goToItemDetails("testuser2");
    cy.findByTestId("view-header").should("contain.text", "testuser2");

    cy.get("#kc-username").should("not.exist");
    cy.findByTestId("email-input").type("testuser1@gmail.com");
    cy.findByTestId("save-user").click();
    masthead.checkNotificationMessage("The user has been saved");
    cy.findByTestId("view-header").should(
      "contain.text",
      "testuser1@gmail.com",
    );

    cy.findByTestId("email-input").clear();
    cy.findByTestId("email-input").type("testuser2@gmail.com");
    cy.findByTestId("save-user").click();
    masthead.checkNotificationMessage("The user has been saved");
    cy.findByTestId("view-header").should(
      "contain.text",
      "testuser2@gmail.com",
    );
  });

  it("Create user with email as username disabled, edit username enabled and user profile disabled", () => {
    // Ensure email as username is disabled and edit username is enabled
    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToLoginTab();

    cy.findByTestId("email-as-username-switch").should("have.value", "off");
    cy.findByTestId("edit-username-switch").check({ force: true });
    cy.findByTestId("edit-username-switch").should("have.value", "on");

    // Create user and check that username is visible and that email is editable
    sidebarPage.goToUsers();
    createUserPage.goToCreateUser();
    cy.get("#kc-username").type("testuser3");
    cy.findByTestId("email-input").type("testuser3@test.com");
    cy.findByTestId("create-user").click();
    masthead.checkNotificationMessage("The user has been created");

    cy.findByTestId("email-input").clear();
    cy.findByTestId("email-input").type("testuser4@test.com");
    cy.findByTestId("save-user").click();
    masthead.checkNotificationMessage("The user has been saved");
    cy.findByTestId("view-header").should("contain.text", "testuser3");
  });

  it("Create user with email as username, edit username enabled and user profile disabled", () => {
    // Create user and check that username is not visible and that email is editable
    sidebarPage.goToUsers();
    createUserPage.goToCreateUser();
    createUserPage.createUser("testuser5");
    cy.findByTestId("email-input").type("testuser5@gmail.com");
    createUserPage.save();
    masthead.checkNotificationMessage("The user has been created");

    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToLoginTab();
    cy.findByTestId("edit-username-switch").check({ force: true });
    cy.findByTestId("edit-username-switch").should("have.value", "on");
    cy.findByTestId("email-as-username-switch").check({ force: true });
    cy.findByTestId("email-as-username-switch").should("have.value", "on");

    //Find user and do some checks
    sidebarPage.goToUsers();
    sidebarPage.waitForPageLoad();
    listingPage.goToItemDetails("testuser5");
    cy.findByTestId("view-header").should("contain.text", "testuser5");
    cy.get("#kc-username").should("not.exist");

    cy.findByTestId("email-input").clear();
    cy.findByTestId("email-input").type("testuser6@test.com");
    cy.findByTestId("save-user").click();
    cy.findByTestId("view-header").should("contain.text", "testuser6@test.com");
    masthead.checkNotificationMessage("The user has been saved");
  });
});
