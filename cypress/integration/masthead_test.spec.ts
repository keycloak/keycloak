import ListingPage from "../support/pages/admin_console/ListingPage";
import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import Masthead from "../support/pages/admin_console/Masthead";
import { keycloakBefore } from "../support/util/keycloak_before";

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
    keycloakBefore();
    loginPage.logIn();
  });

  goToAcctMgtTest();

  it("disables header help and form field help", () => {
    const sidebarPage = new SidebarPage();
    const listingPage = new ListingPage();

    sidebarPage.goToClientScopes();
    listingPage.goToItemDetails("address");

    cy.get("#view-header-subkey").should("exist");
    cy.findByTestId("help-label-name").should("exist");

    masthead.toggleGlobalHelp();

    cy.get("#view-header-subkey").should("not.exist");
    cy.findByTestId("help-label-name").should("not.exist");
  });

  logOutTest();
});

describe("Masthead tests with kebab menu", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    masthead.setMobileMode(true);
  });

  it("shows kabab and hides regular menu", () => {
    masthead.checkKebabShown();
  });

  // TODO: Add test for help when using kebab menu.
  //       Feature not yet implemented for kebab.

  goToAcctMgtTest();
  logOutTest();
});
