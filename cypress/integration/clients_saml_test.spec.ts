import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import ModalUtils from "../support/util/ModalUtils";
import AdminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_before";
import AuthenticationTab from "../support/pages/admin_console/manage/clients/Authentication";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const modalUtils = new ModalUtils();

describe("Clients SAML tests", () => {
  describe("SAML test", () => {
    const samlClientName = "saml";

    before(() => {
      new AdminClient().createClient({
        protocol: "saml",
        clientId: samlClientName,
        publicClient: false,
      });
    });

    after(() => {
      new AdminClient().deleteClient(samlClientName);
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      listingPage.searchItem(samlClientName).goToItemDetails(samlClientName);
    });

    it("should display the saml sections on details screen", () => {
      cy.get(".pf-c-jump-links__list").should(($ul) => {
        expect($ul)
          .to.contain("SAML capabilities")
          .to.contain("Signature and Encryption");
      });
    });

    it("should save force name id format", () => {
      cy.get(".pf-c-jump-links__list").contains("SAML capabilities").click();

      cy.findByTestId("forceNameIdFormat").click({
        force: true,
      });
      cy.findByTestId("settingsSave").click();
      masthead.checkNotificationMessage("Client successfully updated");
    });
  });

  describe("SAML keys tab", () => {
    const clientId = "saml-keys";

    before(() => {
      new AdminClient().createClient({
        clientId,
        protocol: "saml",
      });
    });

    after(() => {
      new AdminClient().deleteClient(clientId);
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      listingPage.searchItem(clientId).goToItemDetails(clientId);
      cy.get("#pf-tab-keys-keys").click();
    });

    it("doesn't disable when no", () => {
      cy.findByTestId("clientSignature").click({ force: true });

      modalUtils
        .checkModalTitle('Disable "Client signature required"')
        .cancelModal();

      cy.findAllByTestId("certificate").should("have.length", 1);
    });

    it("disable client signature", () => {
      cy.findByTestId("clientSignature").click({ force: true });

      modalUtils
        .checkModalTitle('Disable "Client signature required"')
        .confirmModal();

      masthead.checkNotificationMessage("Client successfully updated");
      cy.findAllByTestId("certificate").should("have.length", 0);
    });

    it("should enable Encryption keys config", () => {
      cy.findByTestId("encryptAssertions").click({ force: true });

      cy.findByTestId("generate").click();
      masthead.checkNotificationMessage(
        "New key pair and certificate generated successfully"
      );

      modalUtils.confirmModal();
      cy.findAllByTestId("certificate").should("have.length", 1);
    });
  });

  describe("Authentication tab", () => {
    const clientName = "authenticationTabClient";
    const authenticationTab = new AuthenticationTab();
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
    });

    before(async () => {
      await new AdminClient().createClient({
        protocol: "openid-connect",
        clientId: clientName,
        publicClient: false,
        authorizationServicesEnabled: true,
        serviceAccountsEnabled: true,
        standardFlowEnabled: true,
      });
    });

    after(() => {
      new AdminClient().deleteClient(clientName);
    });

    it("Should update the resource server settings", () => {
      listingPage.searchItem(clientName).goToItemDetails(clientName);
      authenticationTab.goToTab();
      authenticationTab.setPolicy("DISABLED").saveSettings();

      masthead.checkNotificationMessage("Resource successfully updated");
    });

    it("Should create a resource", () => {
      listingPage.searchItem(clientName).goToItemDetails(clientName);
      authenticationTab.goToTab().goToResourceSubTab();
      authenticationTab.assertDefaultResource();

      authenticationTab
        .goToCreateResource()
        .fillResourceForm({
          name: "Resource",
          displayName: "The display name",
          type: "type",
          uris: ["one", "two"],
        })
        .save();

      masthead.checkNotificationMessage("Resource created successfully");
    });
  });
});
