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
import { keycloakBefore } from "../support/util/keycloak_hooks";
import RoleMappingTab from "../support/pages/admin_console/manage/RoleMappingTab";
import KeysTab from "../support/pages/admin_console/manage/clients/KeysTab";
import ClientScopesTab from "../support/pages/admin_console/manage/clients/ClientScopesTab";
import CreateRealmRolePage from "../support/pages/admin_console/manage/realm_roles/CreateRealmRolePage";
import AssociatedRolesPage from "../support/pages/admin_console/manage/realm_roles/AssociatedRolesPage";
import ClientRolesTab from "../support/pages/admin_console/manage/clients/ClientRolesTab";

let itemId = "client_crud";
const loginPage = new LoginPage();
const associatedRolesPage = new AssociatedRolesPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const createClientPage = new CreateClientPage();
const modalUtils = new ModalUtils();
const createRealmRolePage = new CreateRealmRolePage();

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

    it("Should list client scopes", () => {
      listingPage.itemsGreaterThan(1).itemExist(clientScopeName + 0);
    });

    it("Should search existing client scope by name", () => {
      listingPage
        .searchItem(clientScopeName + 0, false)
        .itemExist(clientScopeName + 0)
        .itemsEqualTo(2);
    });

    it("Should search non-existent client scope by name", () => {
      const itemName = "non-existent-item";
      listingPage.searchItem(itemName, false).checkTableExists(false);
    });

    it("Should search existing client scope by assigned type", () => {
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

    /*it("Should empty search", () => {

    });*/

    const newItemsWithExpectedAssignedTypes = [
      [clientScopeNameOptionalType, FilterAssignedType.Optional],
      [clientScopeNameDefaultType, FilterAssignedType.Default],
    ];
    newItemsWithExpectedAssignedTypes.forEach(($type) => {
      const [itemName, assignedType] = $type;
      it(`Should add client scope ${itemName} with ${assignedType} assigned type`, () => {
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
      it(`Should change item ${itemName} AssignedType to ${$assignedType} from search bar`, () => {
        listingPage
          .searchItem(itemName, false)
          .clickItemCheckbox(itemName)
          .changeTypeToOfSelectedItems($assignedType);
        masthead.checkNotificationMessage("Scope mapping updated");
        listingPage.searchItem(itemName, false).itemExist($assignedType);
      });
    });

    it("Should show items on next page are more than 11", () => {
      listingPage.showNextPageTableItems().itemsGreaterThan(1);
    });

    it("Should remove client scope from item bar", () => {
      const itemName = clientScopeName + 0;
      listingPage.searchItem(itemName, false).removeItem(itemName);
      masthead.checkNotificationMessage(msgScopeMappingRemoved);
      listingPage.searchItem(itemName, false).checkTableExists(false);
    });

    /*it("Should remove client scope from search bar", () => {
      //covered by next test
    });*/

    // TODO: https://github.com/keycloak/keycloak-admin-ui/issues/1854
    it("Should remove multiple client scopes from search bar", () => {
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
    /* it("Should show initial items after filtering", () => { 
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

    it("Should check settings elements", () => {
      listingPage.goToCreateItem();
      const clientId = "Test settings";

      createClientPage
        .fillClientData(clientId)
        .continue()
        .checkCapabilityConfigElements()
        .save();

      masthead.checkNotificationMessage("Client created successfully");
      sidebarPage.waitForPageLoad();

      createClientPage
        .checkCapabilityConfigElements()
        .checkAccessSettingsElements()
        .checkLoginSettingsElements()
        .checkLogoutSettingsElements()
        .deleteClientFromActionDropdown();

      modalUtils.confirmModal();
      listingPage.itemExist(clientId, false);
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
      createClientPage.cancel();
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

    it("Initial access token can't be created with 0 days and count", () => {
      const initialAccessTokenTab = new InitialAccessTokenTab();
      initialAccessTokenTab
        .goToInitialAccessTokenTab()
        .shouldBeEmpty()
        .goToCreateFromEmptyList()
        .fillNewTokenData(0, 0)
        .checkExpirationGreaterThanZeroError()
        .checkCountValue(1)
        .checkSaveButtonIsDisabled();
    });

    it("Initial access token", () => {
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
        listingPage.checkRowColumnValue(id, 4, "4").itemExist(id);
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

  describe("Roles tab test", () => {
    const rolesTab = new ClientRolesTab();
    let client: string;

    before(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();

      client = "client_" + (Math.random() + 1).toString(36).substring(7);

      listingPage.goToCreateItem();

      createClientPage
        .selectClientType("openid-connect")
        .fillClientData(client)
        .continue()
        .save();
      masthead.checkNotificationMessage("Client created successfully", true);
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();
      listingPage.searchItem(client).goToItemDetails(client);
      rolesTab.goToRolesTab();
    });

    after(() => {
      adminClient.deleteClient(client);
    });

    it("Should fail to create client role with empty name", () => {
      rolesTab.goToCreateRoleFromEmptyState();
      createRealmRolePage.fillRealmRoleData("").save();
      createRealmRolePage.checkRealmRoleNameRequiredMessage();
    });

    it("Should create client role", () => {
      rolesTab.goToCreateRoleFromEmptyState();
      createRealmRolePage.fillRealmRoleData(itemId).save();
      masthead.checkNotificationMessage("Role created", true);
    });

    it("Should update client role description", () => {
      listingPage.searchItem(itemId, false).goToItemDetails(itemId);
      const updateDescription = "updated description";
      createRealmRolePage.updateDescription(updateDescription).save();
      masthead.checkNotificationMessage("The role has been saved", true);
      createRealmRolePage.checkDescription(updateDescription);
    });

    it("Should add attribute to client role", () => {
      cy.intercept("/admin/realms/master/roles-by-id/*").as("load");
      listingPage.goToItemDetails(itemId);
      rolesTab.goToAttributesTab();
      cy.wait(["@load", "@load"]);
      rolesTab.addAttribute();

      masthead.checkNotificationMessage("The role has been saved", true);
    });

    it("Should delete attribute from client role", () => {
      cy.intercept("/admin/realms/master/roles-by-id/*").as("load");
      listingPage.goToItemDetails(itemId);
      rolesTab.goToAttributesTab();
      cy.wait(["@load", "@load"]);
      rolesTab.deleteAttribute();
      masthead.checkNotificationMessage("The role has been saved", true);
    });

    it("Should create client role to be deleted", () => {
      rolesTab.goToCreateRoleFromToolbar();
      createRealmRolePage.fillRealmRoleData("client_role_to_be_deleted").save();
      masthead.checkNotificationMessage("Role created", true);
    });

    it("Should fail to create duplicate client role", () => {
      rolesTab.goToCreateRoleFromToolbar();
      createRealmRolePage.fillRealmRoleData(itemId).save();
      masthead.checkNotificationMessage(
        `Could not create role: Role with name ${itemId} already exists`,
        true
      );
    });

    it("Should search existing client role", () => {
      listingPage.searchItem(itemId, false).itemExist(itemId);
    });

    it("Should search non-existing role test", () => {
      listingPage.searchItem("role_DNE", false);
      cy.findByTestId(listingPage.emptyState).should("exist");
    });

    it("roles empty search test", () => {
      listingPage.searchItem("", false);
      cy.get("table:visible");
    });

    it("Add associated roles test", () => {
      listingPage.searchItem(itemId, false).goToItemDetails(itemId);

      // Add associated realm role
      associatedRolesPage.addAssociatedRealmRole("create-realm");
      masthead.checkNotificationMessage(
        "Associated roles have been added",
        true
      );

      // Add associated client role
      associatedRolesPage.addAssociatedRoleFromSearchBar("create-client", true);
      masthead.checkNotificationMessage(
        "Associated roles have been added",
        true
      );

      rolesTab.goToAssociatedRolesTab();

      // Add associated client role
      associatedRolesPage.addAssociatedRoleFromSearchBar(
        "manage-consent",
        true
      );
      masthead.checkNotificationMessage(
        "Associated roles have been added",
        true
      );
    });

    it("Should hide inherited roles test", () => {
      listingPage.searchItem(itemId, false).goToItemDetails(itemId);
      rolesTab.goToAssociatedRolesTab();
      rolesTab.hideInheritedRoles();
    });

    it("Should delete associated roles test", () => {
      listingPage.searchItem(itemId, false).goToItemDetails(itemId);
      rolesTab.goToAssociatedRolesTab();
      listingPage.removeItem("create-realm");
      sidebarPage.waitForPageLoad();
      modalUtils.checkModalTitle("Remove associated role?").confirmModal();
      sidebarPage.waitForPageLoad();

      masthead.checkNotificationMessage(
        "Associated roles have been removed",
        true
      );

      listingPage.removeItem("manage-consent");
      sidebarPage.waitForPageLoad();
      modalUtils.checkModalTitle("Remove associated role?").confirmModal();
    });

    it("Should delete associated role from search bar test", () => {
      listingPage.searchItem(itemId, false).goToItemDetails(itemId);
      sidebarPage.waitForPageLoad();
      rolesTab.goToAssociatedRolesTab();

      cy.get('td[data-label="Role name"]')
        .contains("create-client")
        .parent()
        .within(() => {
          cy.get("input").click();
        });

      associatedRolesPage.removeAssociatedRoles();

      sidebarPage.waitForPageLoad();
      modalUtils.checkModalTitle("Remove associated roles?").confirmModal();
      sidebarPage.waitForPageLoad();

      masthead.checkNotificationMessage(
        "Associated roles have been removed",
        true
      );
    });

    it("Should delete client role test", () => {
      listingPage.deleteItem(itemId);
      sidebarPage.waitForPageLoad();
      modalUtils.checkModalTitle("Delete role?").confirmModal();
    });

    it("Should delete client role from role details test", () => {
      listingPage
        .searchItem("client_role_to_be_deleted", false)
        .goToItemDetails("client_role_to_be_deleted");
      createRealmRolePage.clickActionMenu("Delete this role");
      modalUtils.confirmModal();
      masthead.checkNotificationMessage("The role has been deleted", true);
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
      sidebarPage.goToClients();

      client = "client_" + (Math.random() + 1).toString(36).substring(7);

      listingPage.goToCreateItem();

      createClientPage
        .selectClientType("openid-connect")
        .fillClientData(client)
        .continue();

      sidebarPage.waitForPageLoad();

      createClientPage.save();

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
    const serviceAccountTab = new RoleMappingTab("user");
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

    it.skip("Assign", () => {
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
