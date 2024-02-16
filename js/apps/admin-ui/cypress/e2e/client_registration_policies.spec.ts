import ListingPage from "../support/pages/admin-ui/ListingPage";
import { ClientRegistrationPage } from "../support/pages/admin-ui/manage/clients/ClientRegistrationPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const listingPage = new ListingPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const clientRegistrationPage = new ClientRegistrationPage();

describe("Client registration policies tab", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToClients();
    clientRegistrationPage.goToClientRegistrationTab();
    sidebarPage.waitForPageLoad();
  });

  describe("Anonymous client policies subtab", () => {
    it("check anonymous clients list is not empty", () => {
      cy.findByTestId("clientRegistration-anonymous")
        .find("tr")
        .should("have.length.gt", 0);
    });

    it("add anonymous client registration policy", () => {
      clientRegistrationPage
        .createAnonymousPolicy()
        .selectRow("max-clients")
        .fillPolicyForm({
          name: "newAnonymPolicy1",
        })
        .formUtils()
        .save();

      masthead.checkNotificationMessage(
        "New client policy created successfully",
      );
      clientRegistrationPage.formUtils().cancel();
      listingPage.itemExist("newAnonymPolicy1");
    });

    it("edit anonymous client registration policy", () => {
      const policy = "newAnonymPolicy1";
      clientRegistrationPage.findAndSelectInAnonymousPoliciesTable(policy);
      cy.findByTestId("name").clear();
      clientRegistrationPage
        .fillPolicyForm({
          name: "policy2",
        })
        .formUtils()
        .save();

      masthead.checkNotificationMessage("Client policy updated successfully");
      clientRegistrationPage.formUtils().cancel();
      listingPage.itemExist("policy2");
    });

    it("delete anonymous client registration policy", () => {
      const policy = "policy2";
      listingPage.deleteItem(policy);
      cy.findByTestId("confirm").click();

      masthead.checkNotificationMessage(
        "Client registration policy deleted successfully",
      );
    });
  });

  describe("Authenticated client policies subtab", () => {
    beforeEach(() => {
      clientRegistrationPage.goToAuthenticatedSubTab();
      sidebarPage.waitForPageLoad();
    });

    it("check authenticated clients list is not empty", () => {
      cy.findByTestId("clientRegistration-authenticated")
        .find("tr")
        .should("have.length.gt", 0);
    });

    it("add authenticated client registration policy", () => {
      clientRegistrationPage
        .createAuthenticatedPolicy()
        .selectRow("scope")
        .fillPolicyForm({
          name: "newAuthPolicy1",
        })
        .formUtils()
        .save();

      masthead.checkNotificationMessage(
        "New client policy created successfully",
      );
      clientRegistrationPage.formUtils().cancel();
      listingPage.itemExist("newAuthPolicy1");
    });

    it("edit authenticated client registration policy", () => {
      const policy = "newAuthPolicy1";
      clientRegistrationPage.findAndSelectInAuthenticatedPoliciesTable(policy);
      cy.findByTestId("name").clear();
      clientRegistrationPage
        .fillPolicyForm({
          name: "policy3",
        })
        .formUtils()
        .save();

      masthead.checkNotificationMessage("Client policy updated successfully");
      clientRegistrationPage.formUtils().cancel();
      listingPage.itemExist("policy3");
    });

    it("delete authenticated client registration policy", () => {
      const policy = "policy3";
      listingPage.deleteItem(policy);
      cy.findByTestId("confirm").click();

      masthead.checkNotificationMessage(
        "Client registration policy deleted successfully",
      );
    });
  });
});

describe("Accessibility tests for client registration policies", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToClients();
    clientRegistrationPage.goToClientRegistrationTab();
    sidebarPage.waitForPageLoad();
    cy.injectAxe();
  });

  it("Check a11y violations on load/ client registration policies", () => {
    cy.checkA11y();
  });
});
