import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_before";
import ListingPage from "../support/pages/admin_console/ListingPage";

import CreateProviderPage from "../support/pages/admin_console/manage/identity_providers/CreateProviderPage";
import ModalUtils from "../support/util/ModalUtils";
import OrderDialog from "../support/pages/admin_console/manage/identity_providers/OrderDialog";
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

  const changeSuccessMsg =
    "Successfully changed display order of identity providers";
  const deletePrompt = "Delete provider?";
  const deleteSuccessMsg = "Provider successfully deleted";

  const keycloakServer = Cypress.env("KEYCLOAK_SERVER");
  const discoveryUrl = `${keycloakServer}/auth/realms/master/.well-known/openid-configuration`;
  const samlDiscoveryUrl = `${keycloakServer}/auth/realms/master/protocol/saml/descriptor`;
  const authorizationUrl = `${keycloakServer}/auth/realms/master/protocol/openid-connect/auth`;

  describe("Identity provider creation", () => {
    const identityProviderName = "github";

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
      createProviderPage.fill(identityProviderName, "123").clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(identityProviderName);
    });

    it("should create facebook provider", () => {
      createProviderPage
        .clickCreateDropdown()
        .clickItem("facebook")
        .fill("facebook", "123")
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg);
    });

    it("should change order of providers", () => {
      const orderDialog = new OrderDialog();
      const providers = [identityProviderName, "facebook", "bitbucket"];

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist("facebook");

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(identityProviderName);

      createProviderPage
        .clickCreateDropdown()
        .clickItem("bitbucket")
        .fill("bitbucket", "123")
        .clickAdd();

      cy.wait(2000);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(identityProviderName);

      orderDialog.openDialog().checkOrder(providers);
      orderDialog.moveRowTo("facebook", identityProviderName);

      orderDialog.checkOrder(["bitbucket", identityProviderName, "facebook"]);

      orderDialog.clickSave();
      masthead.checkNotificationMessage(changeSuccessMsg);
    });

    it("should create a oidc provider using discovery url", () => {
      const oidcProviderName = "oidc";
      createProviderPage
        .clickCreateDropdown()
        .clickItem(oidcProviderName)
        .fillDiscoveryUrl(discoveryUrl)
        .shouldBeSuccessful()
        .fill("oidc", "123")
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg);
      createProviderPage.shouldHaveAuthorizationUrl(authorizationUrl);
    });

    it("should create a SAML provider using SSO service url", () => {
      const samlProviderName = "saml";
      createProviderPage
        .clickCreateDropdown()
        .clickItem(samlProviderName)
        .fillDiscoveryUrl(samlDiscoveryUrl)
        .shouldBeSuccessful()
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg);
    });

    it("should delete provider", () => {
      const modalUtils = new ModalUtils();
      listingPage.deleteItem(identityProviderName);
      modalUtils.checkModalTitle(deletePrompt).confirmModal();

      masthead.checkNotificationMessage(deleteSuccessMsg);
    });

    it("should add facebook social mapper", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("facebook");

      addMapperPage.goToMappersTab();

      addMapperPage.emptyStateAddMapper();

      addMapperPage.fillSocialMapper("facebook mapper");

      addMapperPage.saveNewMapper();

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add SAML mapper of type Advanced Attribute to Role", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      addMapperPage.emptyStateAddMapper();

      addMapperPage.addAdvancedAttrToRoleMapper("SAML mapper");

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add SAML mapper of type Username Template Importer", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      addMapperPage.addMapper();

      addMapperPage.addUsernameTemplateImporterMapper(
        "SAML Username Template Importer Mapper"
      );

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add SAML mapper of type Hardcoded User Session Attribute", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      addMapperPage.addMapper();

      addMapperPage.addHardcodedUserSessionAttrMapper(
        "Hardcoded User Session Attribute"
      );

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add SAML mapper of type Attribute Importer", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      addMapperPage.addMapper();

      addMapperPage.addSAMLAttrImporterMapper("Attribute Importer");

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add SAML mapper of type Hardcoded Role", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      addMapperPage.addMapper();

      addMapperPage.addHardcodedRoleMapper("Hardcoded Role");

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add SAML mapper of type Hardcoded Attribute", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      addMapperPage.addMapper();

      addMapperPage.addHardcodedAttrMapper("Hardcoded Attribute");

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add SAML mapper of type SAML Attribute To Role", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      addMapperPage.addMapper();

      addMapperPage.addSAMLAttributeToRoleMapper("SAML Attribute To Role");

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add OIDC mapper of type Attribute Importer", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("oidc");

      addMapperPage.goToMappersTab();

      addMapperPage.emptyStateAddMapper();

      addMapperPage.addOIDCAttrImporterMapper("OIDC Attribute Importer");

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add OIDC mapper of type Claim To Role", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("oidc");

      addMapperPage.goToMappersTab();

      addMapperPage.addMapper();

      addMapperPage.addOIDCClaimToRoleMapper("OIDC Claim to Role");

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should add Social mapper of type Attribute Importer", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("facebook");

      addMapperPage.goToMappersTab();

      addMapperPage.addMapper();

      addMapperPage.fillSocialMapper("facebook attribute importer");

      masthead.checkNotificationMessage(createMapperSuccessMsg);
    });

    it("should edit Username Template Importer mapper", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      listingPage.goToItemDetails("SAML Username Template Importer Mapper");

      addMapperPage.editUsernameTemplateImporterMapper();

      masthead.checkNotificationMessage(saveMapperSuccessMsg);
    });

    it("should edit facebook mapper", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("facebook");

      addMapperPage.goToMappersTab();

      listingPage.goToItemDetails("facebook attribute importer");

      addMapperPage.editSocialMapper();
    });

    it("should delete facebook mapper", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("facebook");

      addMapperPage.goToMappersTab();

      listingPage.deleteItem("facebook attribute importer");

      cy.findByTestId("modalConfirm").click();
    });

    it("should edit SAML mapper", () => {
      sidebarPage.goToIdentityProviders();

      listingPage.goToItemDetails("saml");

      addMapperPage.goToMappersTab();

      listingPage.goToItemDetails("SAML mapper");

      addMapperPage.editSAMLorOIDCMapper();

      masthead.checkNotificationMessage(saveMapperSuccessMsg);
    });

    it("clean up providers", () => {
      const modalUtils = new ModalUtils();

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist("bitbucket").deleteItem("bitbucket");
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist("facebook").deleteItem("facebook");
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist("oidc").deleteItem("oidc");
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist("saml").deleteItem("saml");
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg);
    });
  });
});
