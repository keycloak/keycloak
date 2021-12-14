import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_before";
import ListingPage from "../support/pages/admin_console/ListingPage";
import CreateProviderPage from "../support/pages/admin_console/manage/identity_providers/CreateProviderPage";
import ModalUtils from "../support/util/ModalUtils";
import AddMapperPage from "../support/pages/admin_console/manage/identity_providers/AddMapperPage";

describe("Identity provider test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const masthead = new Masthead();
  const listingPage = new ListingPage();
  const createProviderPage = new CreateProviderPage();
  const addMapperPage = new AddMapperPage();

  const createSuccessMsg = "Identity provider successfully created";
  const createMapperSuccessMsg = "Mapper created successfully.";
  const saveMapperSuccessMsg = "Mapper saved successfully.";

  const deletePrompt = "Delete provider?";
  const deleteSuccessMsg = "Provider successfully deleted";

  const keycloakServer = Cypress.env("KEYCLOAK_SERVER");
  const samlDiscoveryUrl = `${keycloakServer}/auth/realms/master/protocol/saml/descriptor`;

  describe("SAML identity provider creation", () => {
    const identityProviderName = "github";
    const samlProviderName = "saml";
    const secret = "123";

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToIdentityProviders();
    });

    it("should create provider", () => {
      createProviderPage.checkGitHubCardVisible().clickGitHubCard();

      createProviderPage.checkAddButtonDisabled();
      createProviderPage
        .fill(identityProviderName)
        .clickAdd()
        .checkClientIdRequiredMessage(true);
      createProviderPage.fill(identityProviderName, secret).clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(identityProviderName);
    });

    it("should create a SAML provider using SSO service url", () => {
      createProviderPage
        .clickCreateDropdown()
        .clickItem(samlProviderName)
        .fillDiscoveryUrl(samlDiscoveryUrl)
        .shouldBeSuccessful()
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);
    });

    it("should add SAML mapper of type Advanced Attribute to Role", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.emptyStateAddMapper();
      addMapperPage.addAdvancedAttrToRoleMapper("SAML mapper");
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add SAML mapper of type Username Template Importer", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.addUsernameTemplateImporterMapper(
        "SAML Username Template Importer Mapper"
      );
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add SAML mapper of type Hardcoded User Session Attribute", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.addHardcodedUserSessionAttrMapper(
        "Hardcoded User Session Attribute"
      );
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add SAML mapper of type Attribute Importer", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.addSAMLAttrImporterMapper("Attribute Importer");
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add SAML mapper of type Hardcoded Role", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.addHardcodedRoleMapper("Hardcoded Role");
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add SAML mapper of type Hardcoded Attribute", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.addHardcodedAttrMapper("Hardcoded Attribute");
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add SAML mapper of type SAML Attribute To Role", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.addSAMLAttributeToRoleMapper("SAML Attribute To Role");
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should edit Username Template Importer mapper", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      listingPage.goToItemDetails("SAML Username Template Importer Mapper");
      addMapperPage.editUsernameTemplateImporterMapper();
      masthead.checkNotificationMessage(saveMapperSuccessMsg, true);
    });

    it("should edit SAML mapper", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      listingPage.goToItemDetails("SAML mapper");
      addMapperPage.editSAMLorOIDCMapper();
      masthead.checkNotificationMessage(saveMapperSuccessMsg, true);
    });

    it("clean up providers", () => {
      const modalUtils = new ModalUtils();

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(samlProviderName).deleteItem(samlProviderName);
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg, true);
      listingPage
        .itemExist(identityProviderName)
        .deleteItem(identityProviderName);
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg, true);
    });
  });
});
