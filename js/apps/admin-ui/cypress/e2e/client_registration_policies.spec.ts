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
        .createPolicy()
        .selectRow("max-clients")
        .fillPolicyForm({
          name: "new policy",
        })
        .formUtils()
        .save();

      masthead.checkNotificationMessage(
        "New client policy created successfully",
      );
      clientRegistrationPage.formUtils().cancel();
      listingPage.itemExist("new policy");
    });

    it("edit anonymous client registration policy", () => {
      listingPage.goToItemDetails("new policy");
      cy.findByTestId("name").clear();
      clientRegistrationPage
        .fillPolicyForm({
          name: "policy 2",
        })
        .formUtils()
        .save();

      masthead.checkNotificationMessage("Client policy updated successfully");
      clientRegistrationPage.formUtils().cancel();
      listingPage.itemExist("policy 2");
    });

    it("delete anonymous client registration policy", () => {
      listingPage.clickRowDetails("policy 2").clickDetailMenu("Delete");
      clientRegistrationPage.modalUtils().confirmModal();

      masthead.checkNotificationMessage(
        "Client registration policy deleted successfully",
      );
      listingPage.itemExist("policy 2", false);
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
        .createPolicy()
        .selectRow("scope")
        .fillPolicyForm({
          name: "new authenticated policy",
        })
        .formUtils()
        .save();

      masthead.checkNotificationMessage(
        "New client policy created successfully",
      );
      clientRegistrationPage.formUtils().cancel();
      listingPage.itemExist("new authenticated policy");
    });

    it("edit authenticated client registration policy", () => {
      listingPage.goToItemDetails("new authenticated policy");
      cy.findByTestId("name").clear();
      clientRegistrationPage
        .fillPolicyForm({
          name: "policy 3",
        })
        .formUtils()
        .save();

      masthead.checkNotificationMessage("Client policy updated successfully");
      clientRegistrationPage.formUtils().cancel();
      listingPage.itemExist("policy 3");
    });

    it("delete authenticated client registration policy", () => {
      listingPage.clickRowDetails("policy 3").clickDetailMenu("Delete");
      clientRegistrationPage.modalUtils().confirmModal();

      masthead.checkNotificationMessage(
        "Client registration policy deleted successfully",
      );
      listingPage.itemExist("policy 3", false);
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
