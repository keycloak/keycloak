import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateClientPage from "../support/pages/admin_console/manage/clients/CreateClientPage";
import ModalUtils from "../support/util/ModalUtils";
import AdvancedTab from "../support/pages/admin_console/manage/clients/AdvancedTab";
import AdminClient from "../support/util/AdminClient";
import InitialAccessTokenTab from "../support/pages/admin_console/manage/clients/InitialAccessTokenTab";
import { keycloakBefore } from "../support/util/keycloak_before";
import RoleMappingTab from "../support/pages/admin_console/manage/RoleMappingTab";
import KeysTab from "../support/pages/admin_console/manage/clients/KeysTab";

let itemId = "client_crud";
const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const createClientPage = new CreateClientPage();
const modalUtils = new ModalUtils();

describe("Clients test", () => {
  describe("Client creation", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
    });

    it("should fail creating client", () => {
      listingPage.goToCreateItem();

      createClientPage.continue().checkClientIdRequiredMessage();

      createClientPage
        .fillClientData("")
        .selectClientType("openid-connect")
        .continue()
        .checkClientIdRequiredMessage();

      createClientPage.fillClientData("account").continue().continue();

      // The error should inform about duplicated name/id
      masthead.checkNotificationMessage(
        "Could not create client: 'Client account already exists'"
      );
    });

    it("Client CRUD test", () => {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);

      // Create
      listingPage.itemExist(itemId, false).goToCreateItem();

      createClientPage
        .selectClientType("openid-connect")
        .fillClientData(itemId)
        .continue()
        .continue();

      masthead.checkNotificationMessage("Client created successfully");

      sidebarPage.goToClients();

      listingPage.searchItem(itemId).itemExist(itemId);

      // Delete
      listingPage.deleteItem(itemId);
      modalUtils.checkModalTitle(`Delete ${itemId} ?`).confirmModal();

      masthead.checkNotificationMessage("The client has been deleted");

      listingPage.itemExist(itemId, false);
    });

    it("Initial access token", () => {
      const initialAccessTokenTab = new InitialAccessTokenTab();
      initialAccessTokenTab.goToInitialAccessTokenTab().shouldBeEmpty();
      initialAccessTokenTab.createNewToken(1, 1).save();

      modalUtils.checkModalTitle("Initial access token details").closeModal();

      initialAccessTokenTab.shouldNotBeEmpty();

      initialAccessTokenTab.getFistId((id) => {
        listingPage.deleteItem(id);
        modalUtils
          .checkModalTitle("Delete initial access token?")
          .confirmModal();
        masthead.checkNotificationMessage(
          "initial access token created successfully"
        );
      });
    });
  });

  describe("Advanced tab test", () => {
    const advancedTab = new AdvancedTab();
    let client: string;

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();

      client = "client_" + (Math.random() + 1).toString(36).substring(7);

      listingPage.goToCreateItem();

      createClientPage
        .selectClientType("openid-connect")
        .fillClientData(client)
        .continue()
        .continue();

      advancedTab.goToTab();
    });

    afterEach(() => {
      new AdminClient().deleteClient(client);
    });

    it("Clustering", () => {
      advancedTab.expandClusterNode();

      advancedTab
        .clickRegisterNodeManually()
        .fillHost("localhost")
        .clickSaveHost();
      advancedTab.checkTestClusterAvailability(true);
    });

    it("Fine grain OpenID connect configuration", () => {
      const algorithm = "ES384";
      advancedTab
        .selectAccessTokenSignatureAlgorithm(algorithm)
        .clickSaveFineGrain();

      advancedTab
        .selectAccessTokenSignatureAlgorithm("HS384")
        .clickRevertFineGrain();
      advancedTab.checkAccessTokenSignatureAlgorithm(algorithm);
    });
  });

  describe("Service account tab test", () => {
    const serviceAccountTab = new RoleMappingTab();
    const serviceAccountName = "service-account-client";

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
    });

    before(async () => {
      await new AdminClient().createClient({
        protocol: "openid-connect",
        clientId: serviceAccountName,
        publicClient: false,
        authorizationServicesEnabled: true,
        serviceAccountsEnabled: true,
        standardFlowEnabled: true,
      });
    });

    after(() => {
      new AdminClient().deleteClient(serviceAccountName);
    });

    it("list", () => {
      listingPage
        .searchItem(serviceAccountName)
        .goToItemDetails(serviceAccountName);
      serviceAccountTab
        .goToServiceAccountTab()
        .checkRoles(["manage-account", "offline_access", "uma_authorization"]);
    });

    /* this test causes the test(s) that follow it to fail - it should be rewritten
    it("assign", () => {
      listingPage.goToItemDetails(serviceAccountName);
      serviceAccountTab
        .goToServiceAccountTab()
        .clickAssignRole(false)
        .selectRow("create-realm")
        .clickAssign();
      masthead.checkNotificationMessage("Role mapping updated");
    });
    */
  });

  describe("Mapping tab", () => {
    const mappingClient = "mapping-client";
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      listingPage.searchItem(mappingClient).goToItemDetails(mappingClient);
    });

    before(() => {
      new AdminClient().createClient({
        protocol: "openid-connect",
        clientId: mappingClient,
        publicClient: false,
      });
    });

    after(() => {
      new AdminClient().deleteClient(mappingClient);
    });

    it("add mapping to openid client", () => {
      cy.get("#pf-tab-mappers-mappers").click();
      cy.findByText("Add predefined mapper").click();
      cy.get("table input").first().click();
      cy.findByTestId("modalConfirm").click();
      masthead.checkNotificationMessage("Mapping successfully created");
    });
  });

  describe("Keys tab test", () => {
    const keysName = "keys-client";
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      listingPage.searchItem(keysName).goToItemDetails(keysName);
    });

    before(() => {
      new AdminClient().createClient({
        protocol: "openid-connect",
        clientId: keysName,
        publicClient: false,
      });
    });

    after(() => {
      new AdminClient().deleteClient(keysName);
    });

    it("change use JWKS Url", () => {
      const keysTab = new KeysTab();
      keysTab.goToTab().checkSaveDisabled();
      keysTab.toggleUseJwksUrl().checkSaveDisabled(false);
    });

    it("generate new keys", () => {
      const keysTab = new KeysTab();
      keysTab.goToTab().clickGenerate();

      keysTab.fillGenerateModal("keyname", "123", "1234").clickConfirm();

      masthead.checkNotificationMessage(
        "New key pair and certificate generated successfully"
      );
    });
  });

  describe("Realm client", () => {
    const clientName = "master-realm";

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      listingPage.searchItem(clientName).goToItemDetails(clientName);
    });

    it("displays the correct tabs", () => {
      cy.findByTestId("client-tabs")
        .find("#pf-tab-settings-settings")
        .should("exist");

      cy.findByTestId("client-tabs")
        .find("#pf-tab-roles-roles")
        .should("exist");

      cy.findByTestId("client-tabs")
        .find("#pf-tab-advanced-advanced")
        .should("exist");

      cy.findByTestId("client-tabs").find("li").should("have.length", 3);
    });

    it("hides the delete action", () => {
      cy.findByTestId("action-dropdown").click();
      cy.findByTestId("delete-client").should("not.exist");
    });
  });

  describe("Bearer only", () => {
    const clientId = "bearer-only";

    before(() => {
      new AdminClient().createClient({
        clientId,
        protocol: "openid-connect",
        publicClient: false,
        bearerOnly: true,
      });
    });

    after(() => {
      new AdminClient().deleteClient(clientId);
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      cy.intercept("/auth/admin/realms/master/clients/*").as("fetchClient");
      listingPage.searchItem(clientId).goToItemDetails(clientId);
      cy.wait("@fetchClient");
    });

    it("shows an explainer text for bearer only clients", () => {
      cy.findByTestId("bearer-only-explainer-label").trigger("mouseenter");
      cy.findByTestId("bearer-only-explainer-tooltip").should("exist");
    });

    it("hides the capability config section", () => {
      cy.findByTestId("capability-config-form").should("not.exist");
      cy.findByTestId("jump-link-capability-config").should("not.exist");
    });
  });
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
      const load = "auth/admin/realms/master/client-scopes";
      cy.intercept(load).as("load");

      cy.get(".pf-c-jump-links__list").contains("SAML capabilities").click();
      cy.wait("@load");

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
});
