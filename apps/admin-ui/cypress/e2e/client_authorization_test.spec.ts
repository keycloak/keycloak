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

describe("Client authentication subtab", () => {
  const loginPage = new LoginPage();
  const listingPage = new ListingPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const authenticationTab = new AuthorizationTab();
  const clientDetailsPage = new ClientDetailsPage();
  const policiesSubTab = new PoliciesTab();
  const permissionsSubTab = new PermissionsTab();
  const clientId =
    "client-authentication-" + (Math.random() + 1).toString(36).substring(7);

  before(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToClients();
    listingPage.searchItem(clientId).goToItemDetails(clientId);
    clientDetailsPage.goToAuthorizationTab();

    cy.wrap(
      adminClient.createClient({
        protocol: "openid-connect",
        clientId,
        publicClient: false,
        authorizationServicesEnabled: true,
        serviceAccountsEnabled: true,
        standardFlowEnabled: true,
      })
    );
  });

  after(() => {
    adminClient.deleteClient(clientId);
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
      true
    );
    authenticationTab.goToScopesSubTab();
    listingPage.itemExist("The scope");
  });

  it("Should create a policy", () => {
    authenticationTab.goToPoliciesSubTab();
    cy.intercept(
      "GET",
      "/admin/realms/master/clients/*/authz/resource-server/policy/regex/*"
    ).as("get");
    policiesSubTab
      .createPolicy("regex")
      .fillBasePolicyForm({
        name: "Regex policy",
        description: "Policy for regex",
        targetClaim: "I don't know",
        regexPattern: ".*?",
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
      "/admin/realms/master/clients/*/authz/resource-server/policy/client/*"
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
      "/admin/realms/master/clients/*/authz/resource-server/resource?first=0&max=10&permission=false"
    ).as("load");
    masthead.checkNotificationMessage(
      "Successfully created the permission",
      true
    );
    cy.wait(["@load"]);

    sidebarPage.waitForPageLoad();
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
      true
    );
  });
});
