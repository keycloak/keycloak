import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import SessionsPage from "../support/pages/admin_console/manage/sessions/SessionsPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const sessionsPage = new SessionsPage();

describe("Sessions test", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToSessions();
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
