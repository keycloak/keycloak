import { keycloakBefore } from "../support/util/keycloak_hooks";
import adminClient from "../support/util/AdminClient";
import LoginPage from "../support/pages/LoginPage";
import ListingPage from "../support/pages/admin_console/ListingPage";
import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import AuthorizationTab from "../support/pages/admin_console/manage/clients/AuthorizationTab";
import ModalUtils from "../support/util/ModalUtils";

describe("Client authentication subtab", () => {
  const loginPage = new LoginPage();
  const listingPage = new ListingPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const authenticationTab = new AuthorizationTab();
  const clientId =
    "client-authentication-" + (Math.random() + 1).toString(36).substring(7);

  before(() => {
    adminClient.createClient({
      protocol: "openid-connect",
      clientId,
      publicClient: false,
      authorizationServicesEnabled: true,
      serviceAccountsEnabled: true,
      standardFlowEnabled: true,
    });
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToClients();
    listingPage.searchItem(clientId).goToItemDetails(clientId);
    authenticationTab.goToAuthenticationTab();
  });

  after(() => {
    adminClient.deleteClient(clientId);
  });

  it("Should update the resource server settings", () => {
    authenticationTab.setPolicy("DISABLED").saveSettings();
    masthead.checkNotificationMessage("Resource successfully updated");
  });

  it("Should create a resource", () => {
    authenticationTab.goToResourceSubTab();
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
    authenticationTab.cancel();
  });

  it("Should create a scope", () => {
    authenticationTab.goToScopeSubTab();
    authenticationTab
      .goToCreateScope()
      .fillScopeForm({
        name: "The scope",
        displayName: "Display something",
        iconUri: "res://something",
      })
      .save();

    masthead.checkNotificationMessage(
      "Authorization scope created successfully"
    );
    authenticationTab.goToScopeSubTab();
    listingPage.itemExist("The scope");
  });

  it.skip("Should create a policy", () => {
    authenticationTab.goToPolicySubTab();
    cy.intercept(
      "GET",
      "/admin/realms/master/clients/*/authz/resource-server/policy/regex/*"
    ).as("get");
    authenticationTab
      .goToCreatePolicy("regex")
      .fillBasePolicyForm({
        name: "Regex policy",
        description: "Policy for regex",
        targetClaim: "I don't know",
        regexPattern: ".*?",
      })
      .save();

    cy.wait(["@get"]);
    masthead.checkNotificationMessage("Successfully created the policy");
    authenticationTab.cancel();
  });

  it.skip("Should delete a policy", () => {
    authenticationTab.goToPolicySubTab();
    listingPage.deleteItem("Regex policy");
    new ModalUtils().confirmModal();

    masthead.checkNotificationMessage("The Policy successfully deleted");
  });

  it.skip("Should create a client policy", () => {
    authenticationTab.goToPolicySubTab();
    cy.intercept(
      "GET",
      "/admin/realms/master/clients/*/authz/resource-server/policy/client/*"
    ).as("get");
    authenticationTab
      .goToCreatePolicy("client")
      .fillBasePolicyForm({
        name: "Client policy",
        description: "Extra client field",
      })
      .inputClient("master-realm")
      .save();

    cy.wait(["@get"]);
    masthead.checkNotificationMessage("Successfully created the policy");
    authenticationTab.cancel();
  });

  it.skip("Should create a permission", () => {
    authenticationTab.goToPermissionsSubTab();
    authenticationTab
      .goToCreatePermission("resource")
      .fillPermissionForm({
        name: "Permission name",
        description: "Something describing this permission",
      })
      .selectResource("Resource")
      .save();

    masthead.checkNotificationMessage("Successfully created the permission");
    authenticationTab.cancel();
  });

  it.skip("Should copy auth details", () => {
    authenticationTab.goToExportSubTab();
    authenticationTab.copy();

    masthead.checkNotificationMessage("Authorization details copied.");
  });

  it.skip("Should export auth details", () => {
    authenticationTab.goToExportSubTab();
    authenticationTab.export();

    masthead.checkNotificationMessage(
      "Successfully exported authorization details."
    );
  });
});
