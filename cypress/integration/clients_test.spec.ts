import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateClientPage from "../support/pages/admin_console/manage/clients/CreateClientPage";
import ModalUtils from "../support/util/ModalUtils";
import AdvancedTab from "../support/pages/admin_console/manage/clients/AdvancedTab";
import AdminClient from "../support/util/AdminClient";
import InitialAccessTokenTab from "../support/pages/admin_console/manage/clients/InitialAccessTokenTab";
import {
  keycloakBefore,
  keycloakBeforeEach,
} from "../support/util/keycloak_hooks";
import RoleMappingTab from "../support/pages/admin_console/manage/RoleMappingTab";
import KeysTab from "../support/pages/admin_console/manage/clients/KeysTab";
import ClientScopesTab from "../support/pages/admin_console/manage/clients/ClientScopesTab";

let itemId = "client_crud";
const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const createClientPage = new CreateClientPage();
const modalUtils = new ModalUtils();

describe("Clients test", () => {
  describe("Client details - Client scopes subtab", () => {
    const clientScopesTab = new ClientScopesTab();
    const client = new AdminClient();
    const clientId = "client-scopes-subtab-test";
    const clientScopeName = "client-scope-test";
    const clientScope = {
      name: clientScopeName,
      description: "",
      protocol: "openid-connect",
      attributes: {
        "include.in.token.scope": "true",
        "display.on.consent.screen": "true",
        "gui.order": "1",
        "consent.screen.text": "",
      },
    };

    before(async () => {
      client.createClient({
        clientId,
        protocol: "openid-connect",
        publicClient: false,
      });
      for (let i = 0; i < 5; i++) {
        clientScope.name = clientScopeName + i;
        await client.createClientScope(clientScope);
        await client.addDefaultClientScopeInClient(
          clientScopeName + i,
          clientId
        );
      }
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      cy.intercept("/auth/admin/realms/master/clients/*").as("fetchClient");
      listingPage.searchItem(clientId).goToItemDetails(clientId);
      cy.wait("@fetchClient");
      clientScopesTab.goToClientScopesTab();
    });

    after(async () => {
      client.deleteClient(clientId);
      for (let i = 0; i < 5; i++) {
        await client.deleteClientScope(clientScopeName + i);
      }
    });

    it("should show items on next page are more than 11", () => {
      listingPage.showNextPageTableItems();
      cy.get(listingPage.tableRowItem).its("length").should("be.gt", 1);
    });
  });

  describe("Client creation", () => {
    before(() => {
      keycloakBefore();
      loginPage.logIn();
    });

    beforeEach(() => {
      keycloakBeforeEach();
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

    before(() => {
      keycloakBefore();
      loginPage.logIn();
    });

    beforeEach(() => {
      keycloakBeforeEach();
      sidebarPage.goToClients();

      client = "client_" + (Math.random() + 1).toString(36).substring(7);

      listingPage.goToCreateItem();

      createClientPage
        .selectClientType("openid-connect")
        .fillClientData(client)
        .continue()
        .continue();

      advancedTab.goToAdvancedTab();
    });

    afterEach(() => {
      new AdminClient().deleteClient(client);
    });

    it("Clustering", () => {
      advancedTab.expandClusterNode();

      advancedTab.registerNodeManually().fillHost("localhost").saveHost();
      advancedTab.checkTestClusterAvailability(true);
    });

    it("Fine grain OpenID connect configuration", () => {
      const algorithm = "ES384";
      advancedTab
        .selectAccessTokenSignatureAlgorithm(algorithm)
        .saveFineGrain();

      advancedTab
        .selectAccessTokenSignatureAlgorithm("HS384")
        .revertFineGrain();
      advancedTab.checkAccessTokenSignatureAlgorithm(algorithm);
    });
  });

  describe("Service account tab test", () => {
    const serviceAccountTab = new RoleMappingTab();
    const serviceAccountName = "service-account-client";

    before(() => {
      keycloakBefore();
      loginPage.logIn();
      new AdminClient().createClient({
        protocol: "openid-connect",
        clientId: serviceAccountName,
        publicClient: false,
        authorizationServicesEnabled: true,
        serviceAccountsEnabled: true,
        standardFlowEnabled: true,
      });
    });

    beforeEach(() => {
      keycloakBeforeEach();
      sidebarPage.goToClients();
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

    it("assign", () => {
      listingPage.goToItemDetails(serviceAccountName);
      serviceAccountTab
        .goToServiceAccountTab()
        .assignRole(false)
        .selectRow("create-realm")
        .assign();
      masthead.checkNotificationMessage("Role mapping updated");
      serviceAccountTab.selectRow("create-realm").unAssign();
      modalUtils.checkModalTitle("Remove mapping?").confirmModal();
      masthead.checkNotificationMessage("Scope mapping successfully removed");
    });
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
      cy.findByTestId("mappersTab").click();
      cy.findByText("Add predefined mapper").click();
      cy.get("table input").first().click();
      cy.findByTestId("modalConfirm").click();
      masthead.checkNotificationMessage("Mapping successfully created");
    });
  });

  describe("Keys tab test", () => {
    const keysName = "keys-client";
    beforeEach(() => {
      keycloakBeforeEach();
      sidebarPage.goToClients();
      listingPage.searchItem(keysName).goToItemDetails(keysName);
    });

    before(() => {
      keycloakBefore();
      loginPage.logIn();
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

    before(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      listingPage.searchItem(clientName).goToItemDetails(clientName);
    });

    beforeEach(() => {
      keycloakBeforeEach();
    });

    it("displays the correct tabs", () => {
      cy.findByTestId("client-tabs")
        .findByTestId("clientSettingsTab")
        .should("exist");

      cy.findByTestId("client-tabs").findByTestId("rolesTab").should("exist");

      cy.findByTestId("client-tabs")
        .findByTestId("advancedTab")
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
      keycloakBefore();
      loginPage.logIn();
      new AdminClient().createClient({
        clientId,
        protocol: "openid-connect",
        publicClient: false,
        bearerOnly: true,
      });
      sidebarPage.goToClients();
      cy.intercept("/auth/admin/realms/master/clients/*").as("fetchClient");
      listingPage.searchItem(clientId).goToItemDetails(clientId);
      cy.wait("@fetchClient");
    });

    after(() => {
      new AdminClient().deleteClient(clientId);
    });

    beforeEach(() => {
      keycloakBeforeEach();
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
});
