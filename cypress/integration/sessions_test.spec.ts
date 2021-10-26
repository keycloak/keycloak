import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import SessionsPage from "../support/pages/admin_console/manage/sessions/SessionsPage";
import { keycloakBefore } from "../support/util/keycloak_before";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const sessionsPage = new SessionsPage();

describe("Sessions test", () => {
  describe("Session type dropdown", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToSessions();
    });

    it("Check dropdown display and all options exist", () => {
      sessionsPage.shouldDisplay();
      sessionsPage.shouldNotBeEmpty();
    });

    it("Select 'All session types' dropdown option", () => {
      sessionsPage.selectAllSessionsType();
    });

    it("Select 'Regular SSO' dropdown option", () => {
      sessionsPage.selectRegularSSO();
    });

    it("Select 'Offline' dropdown option", () => {
      sessionsPage.selectOffline();
    });

    it("Select 'Direct grant' dropdown option", () => {
      sessionsPage.selectDirectGrant();
    });

    it("Select 'Service account' dropdown option", () => {
      sessionsPage.selectServiceAccount();
    });

    it("Set revocation notBefore", () => {
      sessionsPage.setToNow();
    });

    it("Check if notBefore saved", () => {
      sessionsPage.checkNotBeforeValueExists();
    });

    it("Clear revocation notBefore", () => {
      sessionsPage.clearNotBefore();
    });

    it("Check if notBefore cleared", () => {
      sessionsPage.checkNotBeforeCleared();
    });

    it("logout all sessions", () => {
      sessionsPage.logoutAllSessions();

      cy.get("#kc-page-title").contains("Sign in to your account");
    });
  });
});
