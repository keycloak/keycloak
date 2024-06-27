import { SERVER_URL } from "../support/constants";
import LoginPage from "../support/pages/LoginPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import AddMapperPage from "../support/pages/admin-ui/manage/identity_providers/AddMapperPage";
import CreateProviderPage from "../support/pages/admin-ui/manage/identity_providers/CreateProviderPage";
import ProviderSAMLSettings from "../support/pages/admin-ui/manage/identity_providers/social/ProviderSAMLSettings";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";

describe("SAML identity provider test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const masthead = new Masthead();
  const listingPage = new ListingPage();
  const createProviderPage = new CreateProviderPage();
  const addMapperPage = new AddMapperPage();

  const createSuccessMsg = "Identity provider successfully created";
  const saveSuccessMsg = "Provider successfully updated";

  const createMapperSuccessMsg = "Mapper created successfully.";
  const saveMapperSuccessMsg = "Mapper saved successfully.";

  const deletePrompt = "Delete provider?";
  const deleteSuccessMsg = "Provider successfully deleted.";

  const classRefName = "acClassRef-1";
  const declRefName = "acDeclRef-1";

  const samlDiscoveryUrl = `${SERVER_URL}/realms/master/protocol/saml/descriptor`;
  const samlDisplayName = "saml";

  describe("SAML identity provider creation", () => {
    const samlProviderName = "saml";

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToIdentityProviders();
    });

    it("should create a SAML provider using entity descriptor", () => {
      createProviderPage
        .checkVisible(samlProviderName)
        .clickCard(samlProviderName);
      // createProviderPage.checkAddButtonDisabled();
      createProviderPage
        .fillDisplayName(samlDisplayName)
        .fillDiscoveryUrl(samlDiscoveryUrl)
        .shouldBeSuccessful()
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);
    });

    it("should add auth constraints to existing SAML provider", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      createProviderPage
        .fillAuthnContextClassRefs(classRefName)
        .clickClassRefsAdd()
        .fillAuthnContextDeclRefs(declRefName)
        .clickDeclRefsAdd()
        .clickSave();
      masthead.checkNotificationMessage(saveSuccessMsg, true);
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
        "SAML Username Template Importer Mapper",
      );
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add SAML mapper of type Hardcoded User Session Attribute", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.addHardcodedUserSessionAttrMapper(
        "Hardcoded User Session Attribute",
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

    it("should edit SAML settings", () => {
      const providerSAMLSettings = new ProviderSAMLSettings();

      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails(samlProviderName);
      providerSAMLSettings.disableProviderSwitch();
      sidebarPage.goToIdentityProviders();
      cy.findByText("Disabled");
      listingPage.goToItemDetails(samlProviderName);
      providerSAMLSettings.enableProviderSwitch();

      cy.get(".pf-v5-c-jump-links__list").contains("SAML settings").click();
      providerSAMLSettings.assertIdAndURLFields();
      providerSAMLSettings.assertNameIdPolicyFormat();
      providerSAMLSettings.assertPrincipalType();
      providerSAMLSettings.assertSAMLSwitches();
      providerSAMLSettings.assertSignatureAlgorithm();
      providerSAMLSettings.assertValidateSignatures();
      providerSAMLSettings.assertTextFields();

      cy.get(".pf-v5-c-jump-links__list")
        .contains("Requested AuthnContext Constraints")
        .click();
      providerSAMLSettings.assertAuthnContext();
    });

    it("clean up providers", () => {
      const modalUtils = new ModalUtils();

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(samlProviderName).deleteItem(samlProviderName);
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg, true);
    });
  });
});
