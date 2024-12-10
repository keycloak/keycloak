import ClientDetailsPage from "../support/pages/admin-ui/manage/clients/client_details/ClientDetailsPage";
import AdvancedTab from "../support/pages/admin-ui/manage/clients/client_details/tabs/AdvancedTab";
import CreateClientPage from "../support/pages/admin-ui/manage/clients/CreateClientPage";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import CommonPage from "../support/pages/CommonPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const advancedTab = new AdvancedTab();
const realmSettings = new RealmSettingsPage();
const createClientPage = new CreateClientPage();
const clientDetailsPage = new ClientDetailsPage();
const commonPage = new CommonPage();
const loginPage = new LoginPage();

describe("Advanced tab test", () => {
  const realmName = `clients-realm-${crypto.randomUUID()}`;
  let client: string;

  before(() => adminClient.createRealm(realmName));

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    commonPage.sidebar().goToRealm(realmName);
    commonPage.sidebar().goToClients();
    client = "client_" + crypto.randomUUID();
    commonPage.tableToolbarUtils().createClient();
    createClientPage
      .selectClientType("OpenID Connect")
      .fillClientData(client)
      .continue();

    commonPage.sidebar().waitForPageLoad();

    createClientPage.continue().save();
    commonPage
      .masthead()
      .checkNotificationMessage("Client created successfully");
    clientDetailsPage.goToAdvancedTab();
  });

  after(() => adminClient.deleteRealm(realmName));

  it("Clustering", () => {
    advancedTab.expandClusterNode();

    advancedTab.checkEmptyClusterNode();

    advancedTab.registerNodeManually().fillHost("localhost").saveHost();
    advancedTab.checkTestClusterAvailability(true);
    commonPage.masthead().checkNotificationMessage("Node successfully added");
    advancedTab.deleteClusterNode();
    commonPage.modalUtils().confirmModal();
    commonPage.masthead().checkNotificationMessage("Node successfully removed");
    advancedTab.checkEmptyClusterNode();
  });

  it("Fine grain OpenID connect configuration", () => {
    const algorithm = "ES384";
    advancedTab.selectAccessTokenSignatureAlgorithm(algorithm).saveFineGrain();

    advancedTab.selectAccessTokenSignatureAlgorithm("HS384").revertFineGrain();
    advancedTab.checkAccessTokenSignatureAlgorithm(algorithm);
  });

  it("OIDC Compatibility Modes configuration", () => {
    advancedTab.clickAllCompatibilitySwitch();
    advancedTab.saveCompatibility();
    advancedTab.jumpToCompatability();
    advancedTab.clickExcludeSessionStateSwitch();
    advancedTab.clickUseRefreshTokenForClientCredentialsGrantSwitch();
    advancedTab.revertCompatibility();
  });

  it("Client Offline Session Max", () => {
    configureOfflineSessionMaxInRealmSettings(true);

    cy.findByTestId("token-lifespan-clientOfflineSessionMax").should("exist");

    configureOfflineSessionMaxInRealmSettings(false);

    cy.findByTestId("token-lifespan-clientOfflineSessionMax").should(
      "not.exist",
    );

    function configureOfflineSessionMaxInRealmSettings(enabled: boolean) {
      commonPage.sidebar().goToRealmSettings();
      realmSettings.goToSessionsTab();
      realmSettings.setOfflineSessionMaxSwitch(enabled);
      realmSettings.saveSessions();

      commonPage.sidebar().goToClients();
      commonPage.tableToolbarUtils().searchItem(client);
      commonPage.tableUtils().clickRowItemLink(client);
      clientDetailsPage.goToAdvancedTab();
    }
  });

  it("Advanced settings", () => {
    advancedTab.jumpToAdvanced();

    advancedTab.clickAdvancedSwitches();
    advancedTab.jumpToAdvanced();
    advancedTab.selectKeyForCodeExchangeInput("S256");

    advancedTab.saveAdvanced();
    advancedTab.jumpToAdvanced();
    advancedTab.checkAdvancedSwitchesOn();
    advancedTab.checkKeyForCodeExchangeInput("S256");

    advancedTab.selectKeyForCodeExchangeInput("plain");
    advancedTab.checkKeyForCodeExchangeInput("plain");

    advancedTab.jumpToAdvanced();
    advancedTab.clickAdvancedSwitches();

    advancedTab.revertAdvanced();
    advancedTab.jumpToAdvanced();
    advancedTab.checkKeyForCodeExchangeInput("S256");
    //uncomment when revert button reverts all switches
    //and ACR to LoA Mapping + Default ACR Values
    //advancedTab.checkAdvancedSwitchesOn();
  });

  it("Authentication flow override", () => {
    advancedTab.jumpToAuthFlow();
    advancedTab.selectBrowserFlowInput("browser");
    advancedTab.selectDirectGrantInput("docker auth");
    advancedTab.checkBrowserFlowInput("browser");
    advancedTab.checkDirectGrantInput("docker auth");

    advancedTab.revertAuthFlowOverride();
    advancedTab.jumpToAuthFlow();
    advancedTab.checkBrowserFlowInput("Choose...");
    advancedTab.checkDirectGrantInput("Choose...");
    advancedTab.selectBrowserFlowInput("browser");
    advancedTab.selectDirectGrantInput("docker auth");

    advancedTab.saveAuthFlowOverride();
    advancedTab.selectBrowserFlowInput("first broker login");
    advancedTab.selectDirectGrantInput("first broker login");
    advancedTab.revertAuthFlowOverride();
    //revert doesn't work after saving.
    //advancedTab.CheckBrowserFlowInput("browser");
    //advancedTab.CheckDirectGrantInput("docker auth");
  });
});
