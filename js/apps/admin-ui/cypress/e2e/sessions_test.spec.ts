import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import SessionsPage from "../support/pages/admin-ui/manage/sessions/SessionsPage";
import CommonPage from "../support/pages/CommonPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import PageObject from "../support/pages/admin-ui/components/PageObject";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const sessionsPage = new SessionsPage();
const commonPage = new CommonPage();
const listingPage = new ListingPage();
const page = new PageObject();

describe("Sessions test", () => {
  const admin = "admin";
  const client = "security-admin-console";

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToSessions();
  });

  describe("Sessions list view", () => {
    it("check item values", () => {
      listingPage.searchItem(client, false);
      commonPage
        .tableUtils()
        .checkRowItemExists(admin)
        .checkRowItemExists(client);
    });

    it("go to item accessed clients link", () => {
      listingPage.searchItem(client, false);
      commonPage.tableUtils().clickRowItemLink(client);
    });
  });

  describe("Search", () => {
    it("search existing session", () => {
      listingPage.searchItem(admin, false);
      listingPage.itemExist(admin, true);
      page.assertEmptyStateExist(false);
    });

    it("search non-existant session", () => {
      listingPage.searchItem("non-existant-session", false);
      page.assertEmptyStateExist(true);
    });
  });

  describe("revocation", () => {
    it("Clear revocation notBefore", () => {
      sessionsPage.clearNotBefore();
    });

    it("Check if notBefore cleared", () => {
      sessionsPage.checkNotBeforeCleared();
    });

    it("Set revocation notBefore", () => {
      sessionsPage.setToNow();
    });

    it("Check if notBefore saved", () => {
      sessionsPage.checkNotBeforeValueExists();
    });

    it("Push when URI not configured", () => {
      sessionsPage.pushRevocation();
      commonPage
        .masthead()
        .checkNotificationMessage(
          "No push sent. No admin URI configured or no registered cluster nodes available",
        );
    });
  });

  describe("Accessibility tests for sessions", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToSessions();
      cy.injectAxe();
    });

    it("Check a11y violations on load/ sessions", () => {
      cy.checkA11y();
    });

    it("Check a11y violations on revocation dialog", () => {
      cy.findByTestId("action-dropdown").click();
      cy.findByTestId("revocation").click();
      cy.checkA11y();
      cy.findByTestId("cancel").click();
    });

    it("Check a11y violations on sign out all active sessions dialog", () => {
      cy.findByTestId("action-dropdown").click();
      cy.findByTestId("logout-all").click();
      cy.checkA11y();
      cy.findByTestId("cancel").click();
    });
  });
});
