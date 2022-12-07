import ListingPage from "../support/pages/admin-ui/ListingPage";
import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const masthead = new Masthead();
const listingPage = new ListingPage();
const sidebarPage = new SidebarPage();

const logOutTest = () => {
  sidebarPage.waitForPageLoad();
  masthead.signOut();
  sidebarPage.waitForPageLoad();
  loginPage.isLogInPage();
};

const goToAcctMgtTest = () => {
  sidebarPage.waitForPageLoad();
  masthead.accountManagement();
  cy.get("h1").contains("Welcome to Keycloak account management");
  cy.get("#landingReferrerLink").click({ force: true });
  masthead.checkIsAdminUI();
};

describe("Masthead tests in desktop mode", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
  });

  it("Test dropdown in desktop mode", () => {
    goToAcctMgtTest();

    sidebarPage.goToClientScopes();
    listingPage.goToItemDetails("address");

    cy.get("#view-header-subkey").should("exist");
    cy.findByTestId("help-label-name").should("exist");

    masthead.toggleGlobalHelp();

    cy.get("#view-header-subkey").should("not.exist");
    cy.findByTestId("help-label-name").should("not.exist");

    logOutTest();
  });
});

describe("Masthead tests with kebab menu", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    masthead.setMobileMode(true);
  });

  it("Test dropdown in mobile mode", () => {
    masthead.checkKebabShown();
    goToAcctMgtTest();
    logOutTest();
  });

  // TODO: Add test for help when using kebab menu.
  //       Feature not yet implemented for kebab.
});
