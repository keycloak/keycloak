import LoginPage from "../support/pages/LoginPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import CreateClientPage from "../support/pages/admin-ui/manage/clients/CreateClientPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import RoleMappingTab from "../support/pages/admin-ui/manage/RoleMappingTab";
import ClientRolesTab from "../support/pages/admin-ui/manage/clients/ClientRolesTab";
import ClientDetailsPage, {
  ClientsDetailsTab,
} from "../support/pages/admin-ui/manage/clients/client_details/ClientDetailsPage";
import CommonPage from "../support/pages/CommonPage";
import DedicatedScopesMappersTab from "../support/pages/admin-ui/manage/clients/client_details/DedicatedScopesMappersTab";
import { ClientRegistrationPage } from "../support/pages/admin-ui/manage/clients/ClientRegistrationPage";

let itemId = "client_crud";
const loginPage = new LoginPage();
const createClientPage = new CreateClientPage();
const clientDetailsPage = new ClientDetailsPage();
const commonPage = new CommonPage();
const listingPage = new ListingPage();
const dedicatedScopesMappersTab = new DedicatedScopesMappersTab();

describe("Clients test", () => {
  const realmName = `clients-realm-${crypto.randomUUID()}`;

  before(() => adminClient.createRealm(realmName));

  after(() => adminClient.deleteRealm(realmName));

  describe("Client creation", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      commonPage.sidebar().goToRealm(realmName);
      commonPage.sidebar().goToClients();
    });

    it("Should cancel creating client", () => {
      commonPage.tableToolbarUtils().createClient();

      createClientPage.continue().checkClientIdRequiredMessage();

      createClientPage
        .fillClientData("")
        .selectClientType("OpenID Connect")
        .cancel();

      cy.url().should("not.include", "/add-client");
    });

    it("Should check settings elements", () => {
      commonPage.tableToolbarUtils().clickPrimaryButton();
      const clientId = "Test settings";

      createClientPage
        .fillClientData(clientId)
        .continue()
        .checkCapabilityConfigElements()
        .continue()
        .save();

      commonPage
        .masthead()
        .checkNotificationMessage("Client created successfully");
      commonPage.sidebar().waitForPageLoad();

      createClientPage
        .checkCapabilityConfigElements()
        .checkAccessSettingsElements()
        .checkLoginSettingsElements()
        .checkLogoutSettingsElements()
        .deleteClientFromActionDropdown();

      commonPage.modalUtils().confirmModal();
      commonPage.tableUtils().checkRowItemExists(clientId, false);
    });

    it("Should navigate to previous using 'back' button", () => {
      commonPage.tableToolbarUtils().createClient();

      createClientPage.continue().checkClientIdRequiredMessage();

      createClientPage
        .fillClientData("test_client")
        .selectClientType("OpenID Connect")
        .continue()
        .back()
        .checkGeneralSettingsStepActive();
    });

    it("Should fail creating client", () => {
      commonPage.tableToolbarUtils().createClient();

      createClientPage.continue().checkClientIdRequiredMessage();

      createClientPage
        .fillClientData("")
        .selectClientType("OpenID Connect")
        .continue()
        .checkClientIdRequiredMessage();

      createClientPage.fillClientData("account").continue().continue().save();

      // The error should inform about duplicated name/id
      commonPage
        .masthead()
        .checkNotificationMessage(
          "Could not create client: 'Client account already exists'",
        );
    });

    it("Client CRUD test", () => {
      itemId += "_" + crypto.randomUUID();

      // Create
      commonPage.tableUtils().checkRowItemExists(itemId, false);
      commonPage.tableToolbarUtils().clickPrimaryButton();
      createClientPage.cancel();
      commonPage.tableUtils().checkRowItemExists(itemId, false);
      commonPage.tableToolbarUtils().clickPrimaryButton();

      createClientPage
        .selectClientType("OpenID Connect")
        .fillClientData(itemId)
        .continue()
        .switchClientAuthentication()
        .clickDirectAccess()
        .clickImplicitFlow()
        .clickOAuthDeviceAuthorizationGrant()
        .clickOidcCibaGrant()
        .clickServiceAccountRoles()
        .clickStandardFlow()
        .continue()
        .save();

      commonPage
        .masthead()
        .checkNotificationMessage("Client created successfully");

      commonPage.sidebar().goToClients();

      commonPage.tableToolbarUtils().searchItem("John Doe", false);
      commonPage.emptyState().checkIfExists(true);
      commonPage.tableToolbarUtils().searchItem("");
      commonPage.tableUtils().checkRowItemExists("account");
      commonPage.tableToolbarUtils().searchItem(itemId);
      commonPage.tableUtils().checkRowItemExists(itemId);

      // Delete
      commonPage.tableUtils().selectRowItemAction(itemId, "Delete");
      commonPage.sidebar().waitForPageLoad();
      commonPage
        .modalUtils()
        .checkModalTitle(`Delete ${itemId} ?`)
        .confirmModal();
      commonPage
        .masthead()
        .checkNotificationMessage("The client has been deleted");
      commonPage.tableUtils().checkRowItemExists(itemId, false);
    });

    it("Should fail to create imported client with empty ID", () => {
      commonPage.sidebar().goToClients();
      cy.findByTestId("importClient").click();
      cy.findByTestId("clientId").click();
      cy.findByText("Save").click();
      cy.findByText("Required field");
    });

    const identicalClientId = "identical";

    it("Should fail to create client with same ID", () => {
      commonPage.sidebar().goToClients();
      commonPage.tableToolbarUtils().createClient();

      createClientPage
        .fillClientData(identicalClientId)
        .continue()
        .continue()
        .save();

      commonPage.masthead().closeAllAlertMessages();
      commonPage.sidebar().goToClients();
      cy.findByTestId("importClient").click();
      cy.findByTestId("realm-file").selectFile(
        "cypress/fixtures/partial-import-test-data/import-identical-client.json",
        {
          action: "drag-drop",
        },
      );

      cy.wait(1000);
      //cy.findByTestId("realm-file").contains('"clientId": "identical"')
      cy.findByTestId("clientId").click();
      cy.findByText("Save").click();
      commonPage
        .masthead()
        .checkNotificationMessage(
          "Could not import client: Client identical already exists",
          true,
        );
    });

    it("should delete 'identical' client id", () => {
      commonPage.sidebar().goToClients();
      cy.wrap(null).then(() =>
        adminClient.inRealm(realmName, () =>
          adminClient.deleteClient(identicalClientId),
        ),
      );
    });
  });

  describe("Service account tab test", () => {
    const serviceAccountTab = new RoleMappingTab("user");
    const serviceAccountName = `service-account-client${crypto.randomUUID()}`;
    const createRealmRoleName = `create-realm-${crypto.randomUUID()}`;
    const createRealmRoleType = `roles`;

    before(async () => {
      await adminClient.inRealm(realmName, () =>
        adminClient.createClient({
          protocol: "openid-connect",
          clientId: serviceAccountName,
          publicClient: false,
          authorizationServicesEnabled: true,
          serviceAccountsEnabled: true,
          standardFlowEnabled: true,
        }),
      );
      await adminClient.inRealm(realmName, () =>
        adminClient.createRealmRole({
          name: createRealmRoleName,
        }),
      );
    });

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      commonPage.sidebar().goToRealm(realmName);
      commonPage.sidebar().goToClients();
    });

    after(async () => {
      await adminClient.inRealm(realmName, () =>
        adminClient.deleteClient(serviceAccountName),
      );
      await adminClient.inRealm(realmName, () =>
        adminClient.deleteRealmRole(createRealmRoleName),
      );
    });

    it("List", () => {
      commonPage.tableToolbarUtils().searchItem(serviceAccountName);
      commonPage.tableUtils().clickRowItemLink(serviceAccountName);
      serviceAccountTab
        .goToServiceAccountTab()
        .checkRoles(["offline_access", "uma_authorization"], false)
        .checkRoles([`default-roles-${realmName}`, "uma_protection"])
        .unhideInheritedRoles();

      commonPage.sidebar().waitForPageLoad();

      serviceAccountTab
        .checkRoles([
          `default-roles-${realmName}`,
          "offline_access",
          "uma_authorization",
          "uma_protection",
        ])
        .hideInheritedRoles();

      commonPage.sidebar().waitForPageLoad();

      serviceAccountTab
        .checkRoles(["offline_access", "uma_authorization"], false)
        .checkRoles([`default-roles-${realmName}`, "uma_protection"]);

      listingPage
        .searchItem("testing", false)
        .checkEmptyList()
        .searchItem("", false);

      serviceAccountTab
        .checkRoles(["offline_access", "uma_authorization"], false)
        .checkRoles([`default-roles-${realmName}`, "uma_protection"]);
    });

    it("Assign", () => {
      commonPage.tableUtils().clickRowItemLink(serviceAccountName);
      serviceAccountTab
        .goToServiceAccountTab()
        .assignRole(false)
        .changeRoleTypeFilter(createRealmRoleType)
        .selectRow(createRealmRoleName, true)
        .assign();
      commonPage.masthead().checkNotificationMessage("Role mapping updated");

      serviceAccountTab.selectRow(createRealmRoleName).unAssign();

      commonPage.sidebar().waitForPageLoad();
      commonPage.modalUtils().checkModalTitle("Remove role?").confirmModal();
      commonPage.masthead().checkNotificationMessage("Role mapping updated");

      //cy.intercept(`/admin/realms/${realmName}/users`).as("assignRoles");
      serviceAccountTab
        .checkRoles([createRealmRoleName], false)
        .assignRole(false);

      //cy.wait("@assignRoles");
      commonPage.sidebar().waitForPageLoad();

      serviceAccountTab
        .changeRoleTypeFilter("roles")
        .selectRow("offline_access", true)
        .selectRow(createRealmRoleName, true)
        .assign();

      commonPage.masthead().checkNotificationMessage("Role mapping updated");
      commonPage.sidebar().waitForPageLoad();

      serviceAccountTab.unhideInheritedRoles();

      commonPage.sidebar().waitForPageLoad();

      serviceAccountTab.hideInheritedRoles();

      serviceAccountTab.selectRow("offline_access").unAssign();

      commonPage.modalUtils().confirmModal();

      serviceAccountTab
        .checkRoles(["admin", "offline_access"], false)
        .checkRoles([createRealmRoleName]);

      listingPage.clickRowDetails(createRealmRoleName);
      serviceAccountTab.unAssignFromDropdown();

      commonPage.modalUtils().confirmModal();

      commonPage.sidebar().waitForPageLoad();

      serviceAccountTab.unhideInheritedRoles();

      serviceAccountTab
        .checkRoles([createRealmRoleName], false)
        .checkRoles([
          `default-roles-${realmName}`,
          "offline_access",
          "uma_authorization",
          "uma_protection",
        ]);
    });
  });

  describe("Mapping tab", () => {
    const mappingClient = "mapping-client";

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      commonPage.sidebar().goToRealm(realmName);
      commonPage.sidebar().goToClients();
      commonPage.tableToolbarUtils().searchItem(mappingClient);
      commonPage.tableUtils().clickRowItemLink(mappingClient);
    });

    before(() =>
      adminClient.inRealm(realmName, () =>
        adminClient.createClient({
          protocol: "openid-connect",
          clientId: mappingClient,
          publicClient: false,
        }),
      ),
    );

    after(() =>
      adminClient.inRealm(realmName, () =>
        adminClient.deleteClient(mappingClient),
      ),
    );

    it("Add mapping to openid client", () => {
      clientDetailsPage
        .goToClientScopesTab()
        .clickDedicatedScope(mappingClient);
      dedicatedScopesMappersTab.addPredefinedMapper();
      clientDetailsPage.modalUtils().table().clickHeaderItem(1, "input");
      clientDetailsPage.modalUtils().confirmModal();
      clientDetailsPage
        .masthead()
        .checkNotificationMessage("Mapping successfully created");
    });
  });

  describe("Keys tab test", () => {
    const keysName = "keys-client";

    before(
      async () =>
        await adminClient.inRealm(realmName, () =>
          adminClient.createClient({
            protocol: "openid-connect",
            clientId: keysName,
            publicClient: false,
          }),
        ),
    );

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      commonPage.sidebar().goToRealm(realmName);
      commonPage.sidebar().goToClients();
      commonPage.tableToolbarUtils().searchItem(keysName);
      commonPage.tableUtils().clickRowItemLink(keysName);
    });

    after(() =>
      adminClient.inRealm(realmName, () => adminClient.deleteClient(keysName)),
    );

    it("Generate new keys", () => {
      const keysTab = clientDetailsPage.goToKeysTab();
      keysTab.clickGenerate();
      keysTab.fillGenerateModal("JKS", "keyname", "123", "1234").clickConfirm();

      commonPage
        .masthead()
        .checkNotificationMessage(
          "New key pair and certificate generated successfully",
        );
    });
  });

  describe("Realm client", () => {
    const clientName = `${realmName}-realm`;

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      // Stay in master realm, do not switch to ${realmName} realm
      commonPage.sidebar().goToClients();
      commonPage.tableToolbarUtils().searchItem(clientName);
      commonPage.tableUtils().clickRowItemLink(clientName);
    });

    it("Displays the correct tabs", () => {
      clientDetailsPage.goToSettingsTab();
      clientDetailsPage
        .tabUtils()
        .checkTabExists(ClientsDetailsTab.Settings, true)
        .checkTabExists(ClientsDetailsTab.Roles, true)
        .checkTabExists(ClientsDetailsTab.Sessions, true)
        .checkTabExists(ClientsDetailsTab.Permissions, true)
        .checkTabExists(ClientsDetailsTab.Advanced, true)
        .checkNumberOfTabsIsEqual(5);
    });

    it("Hides the delete action", () => {
      commonPage
        .actionToolbarUtils()
        .clickActionToggleButton()
        .checkActionItemExists("Delete", false);
    });
  });

  describe("Bearer only", () => {
    const clientId = "bearer-only";

    before(
      async () =>
        await adminClient.inRealm(realmName, () =>
          adminClient.createClient({
            clientId,
            protocol: "openid-connect",
            publicClient: false,
            bearerOnly: true,
          }),
        ),
    );

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();

      commonPage.sidebar().goToRealm(realmName);
      commonPage.sidebar().goToClients();
      cy.intercept(`/admin/realms/${realmName}/clients/*`).as("fetchClient");
      commonPage.tableToolbarUtils().searchItem(clientId);
      commonPage.tableUtils().clickRowItemLink(clientId);
      cy.wait("@fetchClient");
    });

    after(() =>
      adminClient.inRealm(realmName, () => adminClient.deleteClient(clientId)),
    );

    it("Shows an explainer text for bearer only clients", () => {
      commonPage
        .actionToolbarUtils()
        .bearerOnlyExplainerLabelElement.trigger("mouseenter");
      commonPage
        .actionToolbarUtils()
        .bearerOnlyExplainerTooltipElement.should("exist");
    });

    it("Hides the capability config section", () => {
      cy.findByTestId("capability-config-form").should("not.exist");
      cy.findByTestId("jump-link-capability-config").should("not.exist");
    });
  });

  describe("Generated home URLs for built-in clients", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      commonPage.sidebar().goToRealm(realmName);
      commonPage.sidebar().goToClients();
    });

    it("Check account-console Home URL", () => {
      cy.findByTestId("client-home-url-account-console").contains("/account/");
    });

    it("Check security-admin-console Home URL", () => {
      cy.findByTestId("client-home-url-security-admin-console").contains(
        "/console/",
      );
    });
  });

  describe("Accessibility tests for clients", () => {
    const clientId = "a11y-client";

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      commonPage.sidebar().goToRealm(realmName);
      commonPage.sidebar().goToClients();
      cy.injectAxe();
    });

    it("Check a11y violations on load/ clients list tab", () => {
      cy.checkA11y();
    });

    it("Check a11y violations on create client page", () => {
      commonPage.sidebar().goToClients();
      commonPage.tableToolbarUtils().createClient();
      createClientPage.fillClientData(clientId);
      cy.checkA11y();

      createClientPage.continue();
      cy.checkA11y();

      createClientPage.continue();
      cy.checkA11y();
    });

    it("Check a11y violations on client details page", () => {
      const rolesTab = new ClientRolesTab();

      commonPage.sidebar().goToClients();
      commonPage.tableToolbarUtils().createClient();
      createClientPage.fillClientData(clientId).continue().continue().save();
      cy.checkA11y();

      rolesTab.goToRolesTab();
      cy.checkA11y();

      clientDetailsPage.goToClientScopesTab();
      cy.checkA11y();

      clientDetailsPage.goToClientScopesEvaluateTab();
      cy.checkA11y();

      clientDetailsPage.goToClientScopesEvaluateEffectiveRoleScopeMappingsTab();
      cy.checkA11y();

      clientDetailsPage.goToClientScopesEvaluateGeneratedAccessTokenTab();
      cy.checkA11y();

      clientDetailsPage.goToClientScopesEvaluateGeneratedIdTokenTab();
      cy.checkA11y();

      clientDetailsPage.goToClientScopesEvaluateGeneratedUserInfoTab();
      cy.checkA11y();

      clientDetailsPage.goToAdvancedTab();
      cy.checkA11y();
    });

    it("Check a11y violations in delete dialog", () => {
      commonPage.tableToolbarUtils().searchItem(clientId, false);
      commonPage.tableUtils().selectRowItemAction(clientId, "Delete");
      cy.checkA11y();
      cy.findAllByTestId("confirm").click();
    });

    it("Check a11y violations on client registration/ anonymous access policies tab", () => {
      const clientRegistration = new ClientRegistrationPage();
      clientRegistration.goToClientRegistrationTab();
      cy.checkA11y();
    });

    it("Check a11y violations on client registration/ authenticated access policies tab", () => {
      const clientRegistration = new ClientRegistrationPage();
      clientRegistration.goToClientRegistrationTab();
      cy.findByTestId("authenticated").click();
      cy.checkA11y();
    });
  });
});
