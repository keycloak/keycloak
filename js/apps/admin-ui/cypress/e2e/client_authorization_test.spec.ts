import { v4 as uuid } from "uuid";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import adminClient from "../support/util/AdminClient";
import LoginPage from "../support/pages/LoginPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import AuthorizationTab from "../support/pages/admin-ui/manage/clients/client_details/tabs/AuthorizationTab";
import ModalUtils from "../support/util/ModalUtils";
import ClientDetailsPage from "../support/pages/admin-ui/manage/clients/client_details/ClientDetailsPage";
import PoliciesTab from "../support/pages/admin-ui/manage/clients/client_details/tabs/authorization_subtabs/PoliciesTab";
import PermissionsTab from "../support/pages/admin-ui/manage/clients/client_details/tabs/authorization_subtabs/PermissionsTab";
import CreateResourcePage from "../support/pages/admin-ui/manage/clients/client_details/CreateResourcePage";

describe("Client authentication subtab", () => {
  const loginPage = new LoginPage();
  const listingPage = new ListingPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const authenticationTab = new AuthorizationTab();
  const clientDetailsPage = new ClientDetailsPage();
  const policiesSubTab = new PoliciesTab();
  const permissionsSubTab = new PermissionsTab();
  const clientId = "client-authentication-" + uuid();

  before(() =>
    adminClient.createClient({
      protocol: "openid-connect",
      clientId,
      publicClient: false,
      authorizationServicesEnabled: true,
      serviceAccountsEnabled: true,
      standardFlowEnabled: true,
    }),
  );

  after(() => {
    adminClient.deleteClient(clientId);
  });

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToClients();
    listingPage.searchItem(clientId).goToItemDetails(clientId);
    clientDetailsPage.goToAuthorizationTab();
  });

  it("Should update the resource server settings", () => {
    policiesSubTab.setPolicy("DISABLED").formUtils().save();
    masthead.checkNotificationMessage("Resource successfully updated", true);
  });

  it("Should create a resource", () => {
    const resourcesSubtab = authenticationTab.goToResourcesSubTab();
    listingPage.assertDefaultResource();
    resourcesSubtab
      .createResource()
      .fillResourceForm({
        name: "Resource",
        displayName: "The display name",
        type: "type",
        uris: ["one", "two"],
      })
      .formUtils()
      .save();
    masthead.checkNotificationMessage("Resource created successfully", true);
    sidebarPage.waitForPageLoad();
    authenticationTab.formUtils().cancel();
  });

  it("Edit a resource", () => {
    authenticationTab.goToResourcesSubTab();
    listingPage.goToItemDetails("Resource");

    new CreateResourcePage()
      .fillResourceForm({
        displayName: "updated",
      })
      .formUtils()
      .save();

    masthead.checkNotificationMessage("Resource successfully updated");
    sidebarPage.waitForPageLoad();
    authenticationTab.formUtils().cancel();
  });

  it("Should create a scope", () => {
    authenticationTab
      .goToScopesSubTab()
      .createAuthorizationScope()
      .fillScopeForm({
        name: "The scope",
        displayName: "Display something",
        iconUri: "res://something",
      })
      .formUtils()
      .save();

    masthead.checkNotificationMessage(
      "Authorization scope created successfully",
      true,
    );
    authenticationTab.goToScopesSubTab();
    listingPage.itemExist("The scope");
  });

  it("Should create a policy", () => {
    authenticationTab.goToPoliciesSubTab();
    cy.intercept(
      "GET",
      "/admin/realms/master/clients/*/authz/resource-server/policy/regex/*",
    ).as("get");
    policiesSubTab
      .createPolicy("regex")
      .fillBasePolicyForm({
        name: "Regex policy",
        description: "Policy for regex",
        targetClaim: "I don't know",
        pattern: ".*?",
      })
      .formUtils()
      .save();

    cy.wait(["@get"]);
    masthead.checkNotificationMessage("Successfully created the policy", true);

    sidebarPage.waitForPageLoad();
    authenticationTab.formUtils().cancel();
  });

  it("Should delete a policy", () => {
    authenticationTab.goToPoliciesSubTab();
    listingPage.deleteItem("Regex policy");
    new ModalUtils().confirmModal();

    masthead.checkNotificationMessage("The Policy successfully deleted", true);
  });

  it("Should create a client policy", () => {
    authenticationTab.goToPoliciesSubTab();
    cy.intercept(
      "GET",
      "/admin/realms/master/clients/*/authz/resource-server/policy/client/*",
    ).as("get");
    policiesSubTab
      .createPolicy("client")
      .fillBasePolicyForm({
        name: "Client policy",
        description: "Extra client field",
      })
      .inputClient("master-realm")
      .formUtils()
      .save();
    cy.wait(["@get"]);
    masthead.checkNotificationMessage("Successfully created the policy", true);

    sidebarPage.waitForPageLoad();
    authenticationTab.formUtils().cancel();
  });

  it("Should create a permission", () => {
    authenticationTab.goToPermissionsSubTab();

    permissionsSubTab.createPermission("resource").fillPermissionForm({
      name: "Permission name",
      description: "Something describing this permission",
    });
    permissionsSubTab.selectResource("Default Resource").formUtils().save();
    cy.intercept(
      "/admin/realms/master/clients/*/authz/resource-server/resource?first=0&max=10&permission=false",
    ).as("load");
    masthead.checkNotificationMessage(
      "Successfully created the permission",
      true,
    );
    authenticationTab.formUtils().cancel();
  });

  it.skip("Should copy auth details", () => {
    const exportTab = authenticationTab.goToExportSubTab();
    sidebarPage.waitForPageLoad();
    exportTab.copy();
    masthead.checkNotificationMessage("Authorization details copied.", true);
  });

  it("Should export auth details", () => {
    const exportTab = authenticationTab.goToExportSubTab();
    sidebarPage.waitForPageLoad();
    exportTab.export();

    masthead.checkNotificationMessage(
      "Successfully exported authorization details.",
      true,
    );
  });

  describe("Client authorization tab access for view-realm-authorization", () => {
    const clientId = "realm-view-authz-client-" + uuid();

    beforeEach(async () => {
      const [, testUser] = await Promise.all([
        adminClient.createRealm("realm-view-authz"),
        adminClient.createUser({
          // Create user in master realm
          username: "test-view-authz-user",
          enabled: true,
          credentials: [{ type: "password", value: "password" }],
        }),
      ]);

      await Promise.all([
        adminClient.addClientRoleToUser(
          testUser.id!,
          "realm-view-authz-realm",
          ["view-realm", "view-users", "view-authorization", "view-clients"],
        ),
        adminClient.createClient({
          realm: "realm-view-authz",
          clientId,
          authorizationServicesEnabled: true,
          serviceAccountsEnabled: true,
          standardFlowEnabled: true,
        }),
      ]);
    });

    after(() =>
      Promise.all([
        adminClient.deleteClient(clientId),
        adminClient.deleteUser("test-view-authz-user"),
        adminClient.deleteRealm("realm-view-authz"),
      ]),
    );

    it("Should view autorization tab", () => {
      sidebarPage.waitForPageLoad();
      masthead.signOut();

      sidebarPage.waitForPageLoad();
      loginPage.logIn("test-view-authz-user", "password");
      keycloakBefore();

      sidebarPage.waitForPageLoad().goToRealm("realm-view-authz");

      cy.reload();

      sidebarPage.waitForPageLoad().goToClients();

      listingPage
        .searchItem(clientId, true, "realm-view-authz")
        .goToItemDetails(clientId);
      clientDetailsPage.goToAuthorizationTab();

      authenticationTab.goToResourcesSubTab();
      sidebarPage.waitForPageLoad();
      listingPage.goToItemDetails("Resource");
      sidebarPage.waitForPageLoad();
      cy.go("back");

      authenticationTab.goToScopesSubTab();
      sidebarPage.waitForPageLoad();
      authenticationTab.goToPoliciesSubTab();
      sidebarPage.waitForPageLoad();
      authenticationTab.goToPermissionsSubTab();
      sidebarPage.waitForPageLoad();
      authenticationTab.goToEvaluateSubTab();
      sidebarPage.waitForPageLoad();
    });
  });

  describe("Accessibility tests for client authorization", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClients();
      listingPage.searchItem(clientId).goToItemDetails(clientId);
      clientDetailsPage.goToAuthorizationTab();
      cy.injectAxe();
    });

    it("Check a11y violations on load/ client authorization", () => {
      cy.checkA11y();
    });
  });
});
