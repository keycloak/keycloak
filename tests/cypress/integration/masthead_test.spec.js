import ListingPage from "../support/pages/admin_console/ListingPage.js";
import LoginPage from "../support/pages/LoginPage.js";
import SidebarPage from "../support/pages/admin_console/SidebarPage.js";
import Masthead from "../support/pages/admin_console/Masthead.js";

const loginPage = new LoginPage();
const masthead = new Masthead();

const logOutTest = () => {
  it("logs out", () => {
    masthead.signOut();
    loginPage.isLogInPage();
  });
};

const goToAcctMgtTest = () => {
  it("opens manage account and returns to admin console", () => {
    masthead.accountManagement();
    cy.contains("Welcome to Keycloak Account Management");
    cy.get("#landingReferrerLink").click({ force: true });
    masthead.isAdminConsole();
  });
};

describe("Masthead tests in desktop mode", () => {
  beforeEach(() => {
    cy.clearCookies();
    cy.visit("");
    loginPage.logIn();
  });

  goToAcctMgtTest();

  it("disables header help and form field help", () => {
    const sidebarPage = new SidebarPage();
    const listingPage = new ListingPage();

    sidebarPage.goToClientScopes();
    listingPage.goToItemDetails("address");

    cy.get("#view-header-subkey").should("exist");
    cy.get(`#${CSS.escape("client-scopes-help:name")}`).should("exist");

    masthead.toggleGlobalHelp();

    cy.get("#view-header-subkey").should("not.exist");
    cy.get(`#${CSS.escape("client-scopes-help:name")}`).should("not.exist");
  });

  logOutTest();
});

describe("Masthead tests with kebab menu", () => {
  beforeEach(() => {
    cy.clearCookies();
    cy.visit("");
    loginPage.logIn();
    masthead.setMobileMode(true);
  });

  it("shows kabab and hides regular menu", () => {
    cy.get(masthead.userDrpDwn).should("not.exist");
    cy.get(masthead.userDrpDwnKebab).should("exist");
  });

  // TODO: Add test for help when using kebab menu.
  //       Feature not yet implemented for kebab.

  goToAcctMgtTest();
  logOutTest();
});
