import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import SessionsPage from "../support/pages/admin-ui/manage/sessions/SessionsPage";
import CommonPage from "../support/pages/CommonPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import GroupPage from "../support/pages/admin-ui/manage/groups/GroupPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const sessionsPage = new SessionsPage();
const commonPage = new CommonPage();
const listingPage = new ListingPage();
const groupPage = new GroupPage();

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
      groupPage.assertNoSearchResultsMessageExist(false);
    });

    it("search non-existant session", () => {
      listingPage.searchItem("non-existant-session", false);
      groupPage.assertNoSearchResultsMessageExist(true);
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
          "No push sent. No admin URI configured or no registered cluster nodes available"
        );
    });
  });
});
