import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import ListingPage from "../support/pages/admin_console/ListingPage";
import CreateProviderPage from "../support/pages/admin_console/manage/identity_providers/CreateProviderPage";
import ModalUtils from "../support/util/ModalUtils";
import AddMapperPage from "../support/pages/admin_console/manage/identity_providers/AddMapperPage";
import ProviderBaseGeneralSettingsPage from "../support/pages/admin_console/manage/identity_providers/ProviderBaseGeneralSettingsPage";
import ProviderBaseAdvancedSettingsPage, {
  ClientAuthentication,
  PromptSelect,
} from "../support/pages/admin_console/manage/identity_providers/ProviderBaseAdvancedSettingsPage";

describe("OIDC identity provider test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const masthead = new Masthead();
  const listingPage = new ListingPage();
  const createProviderPage = new CreateProviderPage();
  const addMapperPage = new AddMapperPage();

  const createSuccessMsg = "Identity provider successfully created";
  const createMapperSuccessMsg = "Mapper created successfully.";

  const deletePrompt = "Delete provider?";
  const deleteSuccessMsg = "Provider successfully deleted.";

  const keycloakServer = Cypress.env("KEYCLOAK_SERVER");
  const discoveryUrl = `${keycloakServer}/realms/master/.well-known/openid-configuration`;
  const authorizationUrl = `${keycloakServer}/realms/master/protocol/openid-connect/auth`;

  describe("OIDC Identity provider creation", () => {
    const oidcProviderName = "oidc";
    const secret = "123";

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToIdentityProviders();
    });

    it("should create an OIDC provider using discovery url", () => {
      createProviderPage
        .checkVisible(oidcProviderName)
        .clickCard(oidcProviderName);

      createProviderPage.checkAddButtonDisabled();

      createProviderPage
        .fillDiscoveryUrl(discoveryUrl)
        .shouldBeSuccessful()
        .fill(oidcProviderName, secret)
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);
      createProviderPage.shouldHaveAuthorizationUrl(authorizationUrl);
    });

    it("should test all settings", () => {
      const providerBaseGeneralSettingsPage =
        new ProviderBaseGeneralSettingsPage();
      const providerBaseAdvancedSettingsPage =
        new ProviderBaseAdvancedSettingsPage();

      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(oidcProviderName);
      //general settings
      cy.findByTestId("displayName").click().type("OIDC");
      cy.findByTestId("jump-link-general-settings").click();
      providerBaseGeneralSettingsPage.typeDisplayOrder("1");
      createProviderPage.clickSave();
      masthead.checkNotificationMessage("Provider successfully updated", true);

      //OIDC Settings and save/revert buttons
      providerBaseAdvancedSettingsPage.assertOIDCUrl("authorization");
      providerBaseAdvancedSettingsPage.assertOIDCUrl("token");
      //OIDC Switches
      providerBaseAdvancedSettingsPage.assertOIDCSignatureSwitch();
      providerBaseAdvancedSettingsPage.assertOIDCPKCESwitch();
      //Client Authentication
      providerBaseAdvancedSettingsPage.assertOIDCClientAuthentication(
        ClientAuthentication.basicAuth
      );
      providerBaseAdvancedSettingsPage.assertOIDCClientAuthentication(
        ClientAuthentication.jwt
      );
      providerBaseAdvancedSettingsPage.assertOIDCClientAuthentication(
        ClientAuthentication.jwtPrivKey
      );
      providerBaseAdvancedSettingsPage.assertOIDCClientAuthentication(
        ClientAuthentication.post
      );
      //OIDC Advanced Settings
      providerBaseAdvancedSettingsPage.assertOIDCSettingsAdvancedSwitches();
      providerBaseAdvancedSettingsPage.selectPromptOption(PromptSelect.none);
      providerBaseAdvancedSettingsPage.selectPromptOption(PromptSelect.consent);
      providerBaseAdvancedSettingsPage.selectPromptOption(PromptSelect.login);
      providerBaseAdvancedSettingsPage.selectPromptOption(PromptSelect.select);
      providerBaseAdvancedSettingsPage.selectPromptOption(
        PromptSelect.unspecified
      );
      //Advanced Settings
      providerBaseAdvancedSettingsPage.assertAdvancedSettings();
    });

    it("should add OIDC mapper of type Attribute Importer", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(oidcProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.emptyStateAddMapper();
      addMapperPage.addOIDCAttrImporterMapper("OIDC Attribute Importer");
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add OIDC mapper of type Claim To Role", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(oidcProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.addOIDCClaimToRoleMapper("OIDC Claim to Role");
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("clean up providers", () => {
      const modalUtils = new ModalUtils();

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(oidcProviderName).deleteItem(oidcProviderName);
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg, true);
    });
  });
});
