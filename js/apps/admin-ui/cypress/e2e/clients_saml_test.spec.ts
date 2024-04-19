import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import ModalUtils from "../support/util/ModalUtils";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import SettingsTab from "../support/pages/admin-ui/manage/clients/client_details/tabs/SettingsTab";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const modalUtils = new ModalUtils();

describe("Clients SAML tests", () => {
  describe("SAML test", () => {
    const samlClientName = "saml";

    before(() => {
      adminClient.createClient({
        protocol: "saml",
        clientId: samlClientName,
        publicClient: false,
      });
    });

    after(() => {
      adminClient.deleteClient(samlClientName);
    });

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClients();
      listingPage.searchItem(samlClientName).goToItemDetails(samlClientName);
    });

    it("should display the saml sections on details screen", () => {
      cy.get(".pf-v5-c-jump-links__list").should(($ul) => {
        expect($ul)
          .to.contain("SAML capabilities")
          .to.contain("Signature and Encryption");
      });
    });

    it("should save force name id format", () => {
      cy.get(".pf-v5-c-jump-links__list").contains("SAML capabilities").click();

      cy.findByTestId("attributes.saml🍺force🍺post🍺binding").click({
        force: true,
      });
      cy.findByTestId("settings-save").click();
      masthead.checkNotificationMessage("Client successfully updated");
    });
  });

  describe("SAML keys tab", () => {
    const clientId = "saml-keys";

    before(() => {
      adminClient.createClient({
        clientId,
        protocol: "saml",
      });
    });

    after(() => {
      adminClient.deleteClient(clientId);
    });

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClients();
      listingPage.searchItem(clientId).goToItemDetails(clientId);
      cy.findByTestId("keysTab").click();
    });

    it("should doesn't disable signature when cancel", () => {
      cy.findByTestId("clientSignature").click({ force: true });

      modalUtils
        .checkModalTitle('Disable "Client signature required"')
        .cancelModal();

      cy.findAllByTestId("certificate").should("have.length", 1);
    });

    it("should disable client signature", () => {
      cy.intercept(
        "admin/realms/master/clients/*/certificates/saml.signing",
      ).as("load");
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
        "New key pair and certificate generated successfully",
      );

      modalUtils.confirmModal();
      cy.findAllByTestId("certificate").should("have.length", 1);
    });
  });

  describe("SAML settings tab", () => {
    const clientId = "saml-settings";
    const settingsTab = new SettingsTab();

    before(() => {
      adminClient.createClient({
        clientId,
        protocol: "saml",
      });
    });

    after(() => {
      adminClient.deleteClient(clientId);
    });

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClients();
      listingPage.searchItem(clientId).goToItemDetails(clientId);
    });

    it("should check SAML capabilities", () => {
      cy.get(".pf-v5-c-jump-links__list").contains("SAML capabilities").click();

      settingsTab.assertNameIdFormatDropdown();
      settingsTab.assertSAMLCapabilitiesSwitches();
    });

    it("should check signature and encryption", () => {
      cy.get(".pf-v5-c-jump-links__list")
        .contains("Signature and Encryption")
        .click();

      settingsTab.assertSignatureAlgorithmDropdown();
      settingsTab.assertSignatureKeyNameDropdown();
      settingsTab.assertCanonicalizationDropdown();

      settingsTab.assertSignatureEncryptionSwitches();
    });

    it("should check access settings", () => {
      cy.get(".pf-v5-c-jump-links__list").contains("Access settings").click();

      const validUrl =
        "http://localhost:8180/realms/master/protocol/" +
        clientId +
        "/clients/";
      const rootUrlError =
        "Client could not be updated: Root URL is not a valid URL";
      const homeUrlError =
        "Client could not be updated: Base URL is not a valid URL";

      cy.findByTestId("rootUrl").type("Invalid URL");
      settingsTab.clickSaveBtn();
      masthead.checkNotificationMessage(rootUrlError);
      cy.findByTestId("rootUrl").clear();

      cy.findByTestId("baseUrl").type("Invalid URL");
      settingsTab.clickSaveBtn();
      masthead.checkNotificationMessage(homeUrlError);
      cy.findByTestId("baseUrl").clear();

      cy.findByTestId("rootUrl").type(validUrl);
      cy.findByTestId("baseUrl").type(validUrl);
      settingsTab.clickSaveBtn();
      masthead.checkNotificationMessage("Client successfully updated");

      settingsTab.assertAccessSettings();
    });

    it("should check login settings", () => {
      cy.get(".pf-v5-c-jump-links__list").contains("Login settings").click();

      settingsTab.assertLoginThemeDropdown();
      settingsTab.assertLoginSettings();
    });
  });
});
