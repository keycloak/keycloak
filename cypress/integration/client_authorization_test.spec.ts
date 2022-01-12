import {
  keycloakBefore,
  keycloakBeforeEach,
} from "../support/util/keycloak_hooks";
import AdminClient from "../support/util/AdminClient";
import LoginPage from "../support/pages/LoginPage";
import ListingPage from "../support/pages/admin_console/ListingPage";
import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import AuthorizationTab from "../support/pages/admin_console/manage/clients/AuthorizationTab";

describe("Client authentication subtab", () => {
  const adminClient = new AdminClient();
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
  });

  after(() => {
    adminClient.deleteClient(clientId);
  });

  beforeEach(() => {
    keycloakBeforeEach();
    sidebarPage.goToClients();
    listingPage.searchItem(clientId).goToItemDetails(clientId);
    authenticationTab.goToAuthenticationTab();
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

  it("Should create a permission", () => {
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
  });
});
