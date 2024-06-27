import { SERVER_URL } from "../support/constants";
import LoginPage from "../support/pages/LoginPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import AddMapperPage from "../support/pages/admin-ui/manage/identity_providers/AddMapperPage";
import CreateProviderPage from "../support/pages/admin-ui/manage/identity_providers/CreateProviderPage";
import ProviderBaseAdvancedSettingsPage, {
  ClientAssertionSigningAlg,
  ClientAuthentication,
  PromptSelect,
} from "../support/pages/admin-ui/manage/identity_providers/ProviderBaseAdvancedSettingsPage";
import ProviderBaseGeneralSettingsPage from "../support/pages/admin-ui/manage/identity_providers/ProviderBaseGeneralSettingsPage";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";

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

  const discoveryUrl = `${SERVER_URL}/realms/master/.well-known/openid-configuration`;
  const authorizationUrl = `${SERVER_URL}/realms/master/protocol/openid-connect/auth`;

  describe("OIDC Identity provider creation", () => {
    const oidcProviderName = "oidc";
    const secret = "123";

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToIdentityProviders();
    });

    it("should create an OIDC provider using discovery url", () => {
      createProviderPage
        .checkVisible(oidcProviderName)
        .clickCard(oidcProviderName);

      // createProviderPage.checkAddButtonDisabled();

      createProviderPage
        .fillDiscoveryUrl(discoveryUrl)
        .shouldBeSuccessful()
        .fillDisplayName(oidcProviderName)
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
        ClientAuthentication.basicAuth,
      );
      providerBaseAdvancedSettingsPage.assertOIDCClientAuthentication(
        ClientAuthentication.post,
      );
      providerBaseAdvancedSettingsPage.assertOIDCClientAuthentication(
        ClientAuthentication.jwt,
      );
      providerBaseAdvancedSettingsPage.assertOIDCClientAuthentication(
        ClientAuthentication.jwtPrivKey,
      );
      //Client assertion signature algorithm
      Object.entries(ClientAssertionSigningAlg).forEach(([, value]) => {
        providerBaseAdvancedSettingsPage.assertOIDCClientAuthSignAlg(value);
      });
      //Client assertion audience
      providerBaseAdvancedSettingsPage.typeClientAssertionAudience(
        "http://localhost:8180",
      );
      providerBaseAdvancedSettingsPage.assertClientAssertionAudienceInputEqual(
        "http://localhost:8180",
      );
      //JWT X509 Headers
      providerBaseAdvancedSettingsPage.assertOIDCJWTX509HeadersSwitch();
      //OIDC Advanced Settings
      providerBaseAdvancedSettingsPage.assertOIDCSettingsAdvancedSwitches();
      providerBaseAdvancedSettingsPage.selectPromptOption(PromptSelect.none);
      providerBaseAdvancedSettingsPage.selectPromptOption(PromptSelect.consent);
      providerBaseAdvancedSettingsPage.selectPromptOption(PromptSelect.login);
      providerBaseAdvancedSettingsPage.selectPromptOption(PromptSelect.select);
      providerBaseAdvancedSettingsPage.selectPromptOption(
        PromptSelect.unspecified,
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

    it("should cancel the addition of the OIDC mapper", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(oidcProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.cancelNewMapper();
      addMapperPage.shouldGoToMappersTab();
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
