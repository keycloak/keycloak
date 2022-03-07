import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage, {
  Filter,
  FilterAssignedType,
} from "../support/pages/admin_console/ListingPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateClientPage from "../support/pages/admin_console/manage/clients/CreateClientPage";
import ModalUtils from "../support/util/ModalUtils";
import AdvancedTab from "../support/pages/admin_console/manage/clients/AdvancedTab";
import adminClient from "../support/util/AdminClient";
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
    const clientId = "client-scopes-subtab-test";
    const clientScopeName = "client-scope-test";
    const clientScopeNameDefaultType = "client-scope-test-default-type";
    const clientScopeNameOptionalType = "client-scope-test-optional-type";
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
    const msgScopeMappingRemoved = "Scope mapping successfully removed";

    before(async () => {
      adminClient.createClient({
        clientId,
        protocol: "openid-connect",
        publicClient: false,
      });
      for (let i = 0; i < 5; i++) {
        clientScope.name = clientScopeName + i;
        await adminClient.createClientScope(clientScope);
        await adminClient.addDefaultClientScopeInClient(
          clientScopeName + i,
          clientId
        );
      }
      clientScope.name = clientScopeNameDefaultType;
      await adminClient.createClientScope(clientScope);
      clientScope.name = clientScopeNameOptionalType;
      await adminClient.createClientScope(clientScope);
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      cy.intercept("/admin/realms/master/clients/*").as("fetchClient");
      listingPage.searchItem(clientId).goToItemDetails(clientId);
      cy.wait("@fetchClient");
      clientScopesTab.goToClientScopesTab();
    });

    after(async () => {
      adminClient.deleteClient(clientId);
      for (let i = 0; i < 5; i++) {
        await adminClient.deleteClientScope(clientScopeName + i);
      }
      await adminClient.deleteClientScope(clientScopeNameDefaultType);
      await adminClient.deleteClientScope(clientScopeNameOptionalType);
    });

    it("should list client scopes", () => {
      listingPage.itemsGreaterThan(1).itemExist(clientScopeName + 0);
    });

    it("should search existing client scope by name", () => {
      listingPage
        .searchItem(clientScopeName + 0, false)
        .itemExist(clientScopeName + 0)
        .itemsEqualTo(2);
    });

    it("should search non-existent client scope by name", () => {
      const itemName = "non-existent-item";
      listingPage.searchItem(itemName, false).checkTableExists(false);
    });

    it("should search existing client scope by assigned type", () => {
      listingPage
        .selectFilter(Filter.AssignedType)
        .selectSecondaryFilterAssignedType(FilterAssignedType.Default)
        .itemExist(FilterAssignedType.Default)
        .itemExist(FilterAssignedType.Optional, false)
        .selectSecondaryFilterAssignedType(FilterAssignedType.Optional)
        .itemExist(FilterAssignedType.Default, false)
        .itemExist(FilterAssignedType.Optional)
        .selectSecondaryFilterAssignedType(FilterAssignedType.AllTypes)
        .itemExist(FilterAssignedType.Default)
        .itemExist(FilterAssignedType.Optional);
    });

    /*it("should empty search", () => {

    });*/

    const newItemsWithExpectedAssignedTypes = [
      [clientScopeNameOptionalType, FilterAssignedType.Optional],
      [clientScopeNameDefaultType, FilterAssignedType.Default],
    ];
    newItemsWithExpectedAssignedTypes.forEach(($type) => {
      const [itemName, assignedType] = $type;
      it(`should add client scope ${itemName} with ${assignedType} assigned type`, () => {
        listingPage.clickPrimaryButton();
        modalUtils.checkModalTitle("Add client scopes to " + clientId);
        listingPage.clickItemCheckbox(itemName);
        modalUtils.confirmModalWithItem(assignedType);
        masthead.checkNotificationMessage("Scope mapping successfully updated");
        listingPage
          .searchItem(itemName, false)
          .itemExist(itemName)
          .itemExist(assignedType);
      });
    });

    const expectedItemAssignedTypes = [
      FilterAssignedType.Optional,
      FilterAssignedType.Default,
    ];
    expectedItemAssignedTypes.forEach(($assignedType) => {
      const itemName = clientScopeName + 0;
      it(`should change item ${itemName} AssignedType to ${$assignedType} from search bar`, () => {
        listingPage
          .searchItem(itemName, false)
          .clickItemCheckbox(itemName)
          .changeTypeToOfSelectedItems($assignedType);
        masthead.checkNotificationMessage("Scope mapping updated");
        listingPage.searchItem(itemName, false).itemExist($assignedType);
      });
    });

    it("should show items on next page are more than 11", () => {
      listingPage.showNextPageTableItems().itemsGreaterThan(1);
    });

    it("should remove client scope from item bar", () => {
      const itemName = clientScopeName + 0;
      listingPage.searchItem(itemName, false).removeItem(itemName);
      masthead.checkNotificationMessage(msgScopeMappingRemoved);
      listingPage.searchItem(itemName, false).checkTableExists(false);
    });

    /*it("should remove client scope from search bar", () => {
      //covered by next test
    });*/

    // TODO: https://github.com/keycloak/keycloak-admin-ui/issues/1854
    it("should remove multiple client scopes from search bar", () => {
      const itemName1 = clientScopeName + 1;
      const itemName2 = clientScopeName + 2;
      listingPage
        .clickSearchBarActionButton()
        .checkDropdownItemIsDisabled("Remove")
        .searchItem(clientScopeName, false)
        .clickItemCheckbox(itemName1)
        .clickItemCheckbox(itemName2)
        .clickSearchBarActionButton()
        .clickSearchBarActionItem("Remove");
      masthead.checkNotificationMessage(msgScopeMappingRemoved);
      listingPage
        .searchItem(clientScopeName, false)
        .itemExist(itemName1, false)
        .itemExist(itemName2, false)
        .clickSearchBarActionButton();
      //.checkDropdownItemIsDisabled("Remove");
    });

    //TODO: https://github.com/keycloak/keycloak-admin-ui/issues/1874
    /* it("should show initial items after filtering", () => { 
      listingPage
        .selectFilter(Filter.AssignedType)
        .selectFilterAssignedType(FilterAssignedType.Optional)
        .selectFilter(Filter.Name)
        .itemExist(FilterAssignedType.Default)
        .itemExist(FilterAssignedType.Optional);
    });*/
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

    it("Should cancel creating client", () => {
      listingPage.goToCreateItem();

      createClientPage.continue().checkClientIdRequiredMessage();

      createClientPage
        .fillClientData("")
        .selectClientType("openid-connect")
        .cancel();

      cy.url().should("not.include", "/add-client");
    });

    it("Should navigate to previous using 'back' button", () => {
      listingPage.goToCreateItem();

      createClientPage.continue().checkClientIdRequiredMessage();

      createClientPage
        .fillClientData("test_client")
        .selectClientType("openid-connect")
        .continue()
        .back()
        .checkGeneralSettingsStepActive();
    });

    it("Should fail creating client", () => {
      listingPage.goToCreateItem();

      createClientPage.continue().checkClientIdRequiredMessage();

      createClientPage
        .fillClientData("")
        .selectClientType("openid-connect")
        .continue()
        .checkClientIdRequiredMessage();

      createClientPage.fillClientData("account").continue().save();

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
        .switchClientAuthentication()
        .clickDirectAccess()
        .clickImplicitFlow()
        .clickOAuthDeviceAuthorizationGrant()
        .clickOidcCibaGrant()
        .clickServiceAccountRoles()
        .clickStandardFlow()
        .save();

      masthead.checkNotificationMessage("Client created successfully");

      sidebarPage.goToClients();

      listingPage.searchItem("John Doe", false).checkEmptyList();
      listingPage.searchItem("").itemExist("account");
      listingPage.searchItem(itemId).itemExist(itemId);

      // Delete
      listingPage.deleteItem(itemId);
      sidebarPage.waitForPageLoad();
      modalUtils.checkModalTitle(`Delete ${itemId} ?`).confirmModal();

      masthead.checkNotificationMessage("The client has been deleted");

      listingPage.itemExist(itemId, false);
    });

    it.skip("Initial access token", () => {
      const initialAccessTokenTab = new InitialAccessTokenTab();
      initialAccessTokenTab
        .goToInitialAccessTokenTab()
        .shouldBeEmpty()
        .goToCreateFromEmptyList()
        .fillNewTokenData(1, 3)
        .save();

      modalUtils.checkModalTitle("Initial access token details").closeModal();

      masthead.checkNotificationMessage(
        "New initial access token has been created"
      );

      initialAccessTokenTab.shouldNotBeEmpty();

      listingPage
        .searchItem("John Doe", false)
        .checkEmptyList()
        .searchItem("", false);

      initialAccessTokenTab.getFirstId((id) => {
        listingPage
          .checkRowColumnValue(id, 4, "3")
          .checkRowColumnValue(id, 5, "3")
          .itemExist(id);
      });

      listingPage.goToCreateItem();
      initialAccessTokenTab.fillNewTokenData(1, 3).save();

      modalUtils.closeModal();

      initialAccessTokenTab.getFirstId((id) => {
        listingPage.deleteItem(id);
        sidebarPage.waitForPageLoad();
        modalUtils
          .checkModalTitle("Delete initial access token?")
          .confirmModal();
      });

      masthead.checkNotificationMessage(
        "Initial access token deleted successfully"
      );
      initialAccessTokenTab.shouldNotBeEmpty();

      initialAccessTokenTab.getFirstId((id) => {
        listingPage.deleteItem(id);
        sidebarPage.waitForPageLoad();
        modalUtils.confirmModal();
      });
      initialAccessTokenTab.shouldBeEmpty();
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
        .save();

      advancedTab.goToAdvancedTab();
    });

    afterEach(() => {
      adminClient.deleteClient(client);
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
      adminClient.createClient({
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
      adminClient.deleteClient(serviceAccountName);
    });

    it("List", () => {
      listingPage
        .searchItem(serviceAccountName)
        .goToItemDetails(serviceAccountName);
      serviceAccountTab
        .goToServiceAccountTab()
        .checkRoles(["manage-account", "offline_access", "uma_authorization"]);
    });

    it("Assign", () => {
      listingPage.goToItemDetails(serviceAccountName);
      serviceAccountTab
        .goToServiceAccountTab()
        .assignRole(false)
        .selectRow("create-realm")
        .assign();
      masthead.checkNotificationMessage("Role mapping updated");
      serviceAccountTab.selectRow("create-realm").unAssign();
      sidebarPage.waitForPageLoad();
      modalUtils.checkModalTitle("Remove mapping?").confirmModal();
      masthead.checkNotificationMessage("Scope mapping successfully removed");
    });
  });

  describe("Mapping tab", () => {
    const clientScopeTab = new ClientScopesTab();
    const mappingClient = "mapping-client";
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      listingPage.searchItem(mappingClient).goToItemDetails(mappingClient);
    });

    before(() => {
      adminClient.createClient({
        protocol: "openid-connect",
        clientId: mappingClient,
        publicClient: false,
      });
    });

    after(() => {
      adminClient.deleteClient(mappingClient);
    });

    it("Add mapping to openid client", () => {
      clientScopeTab.goToClientScopesTab().clickDedicatedScope(mappingClient);
      cy.findByTestId("mappersTab").click();
      cy.findByText("Add predefined mapper").click();
      cy.get("table input").first().click();
      cy.findByTestId("confirm").click();
      masthead.checkNotificationMessage("Mapping successfully created");
    });
  });

  describe("Keys tab test", () => {
    const keysName = "keys-client";
    before(() => {
      keycloakBefore();
      loginPage.logIn();
      adminClient.createClient({
        protocol: "openid-connect",
        clientId: keysName,
        publicClient: false,
      });
    });

    beforeEach(() => {
      keycloakBeforeEach();
      sidebarPage.goToClients();
      listingPage.searchItem(keysName).goToItemDetails(keysName);
    });

    after(() => {
      adminClient.deleteClient(keysName);
    });

    it("Change use JWKS Url", () => {
      const keysTab = new KeysTab();
      keysTab.goToTab().checkSaveDisabled();
      keysTab.toggleUseJwksUrl().checkSaveDisabled(false);
    });

    it("Generate new keys", () => {
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

    it("Displays the correct tabs", () => {
      cy.findByTestId("client-tabs")
        .findByTestId("clientSettingsTab")
        .should("exist");

      cy.findByTestId("client-tabs").findByTestId("rolesTab").should("exist");

      cy.findByTestId("client-tabs")
        .findByTestId("advancedTab")
        .should("exist");

      cy.findByTestId("client-tabs").find("li").should("have.length", 3);
    });

    it("Hides the delete action", () => {
      cy.findByTestId("action-dropdown").click();
      cy.findByTestId("delete-client").should("not.exist");
    });
  });

  describe("Bearer only", () => {
    const clientId = "bearer-only";

    before(() => {
      keycloakBefore();
      loginPage.logIn();
      adminClient.createClient({
        clientId,
        protocol: "openid-connect",
        publicClient: false,
        bearerOnly: true,
      });
      sidebarPage.goToClients();
      cy.intercept("/admin/realms/master/clients/*").as("fetchClient");
      listingPage.searchItem(clientId).goToItemDetails(clientId);
      cy.wait("@fetchClient");
    });

    after(() => {
      adminClient.deleteClient(clientId);
    });

    beforeEach(() => {
      keycloakBeforeEach();
    });

    it("Shows an explainer text for bearer only clients", () => {
      cy.findByTestId("bearer-only-explainer-label").trigger("mouseenter");
      cy.findByTestId("bearer-only-explainer-tooltip").should("exist");
    });

    it("Hides the capability config section", () => {
      cy.findByTestId("capability-config-form").should("not.exist");
      cy.findByTestId("jump-link-capability-config").should("not.exist");
    });
  });
});
