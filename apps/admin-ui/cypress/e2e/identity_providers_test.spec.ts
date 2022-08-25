import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
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

  const changeSuccessMsg =
    "Successfully changed display order of identity providers";
  const deletePrompt = "Delete provider?";
  const deleteSuccessMsg = "Provider successfully deleted.";

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
      masthead.checkNotificationMessage(createSuccessMsg, true);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(identityProviderName);
    });

    it("should create facebook provider", () => {
      createProviderPage
        .clickCreateDropdown()
        .clickItem("facebook")
        .fill("facebook", "123")
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);
    });

    it.skip("should change order of providers", () => {
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
      masthead.checkNotificationMessage(createSuccessMsg, true);

      cy.wait(2000);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(identityProviderName);

      orderDialog.openDialog().checkOrder(providers);
      orderDialog.moveRowTo("facebook", identityProviderName);

      orderDialog.checkOrder(["bitbucket", identityProviderName, "facebook"]);

      orderDialog.clickSave();
      masthead.checkNotificationMessage(changeSuccessMsg);
    });

    it("should delete provider", () => {
      const modalUtils = new ModalUtils();
      listingPage.deleteItem(identityProviderName);
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg, true);
    });

    it("should add facebook social mapper", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails("facebook");
      addMapperPage.goToMappersTab();
      addMapperPage.emptyStateAddMapper();
      addMapperPage.fillSocialMapper("facebook mapper");
      // addMapperPage.saveNewMapper();
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
    });

    it("should add Social mapper of type Attribute Importer", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails("facebook");
      addMapperPage.goToMappersTab();
      addMapperPage.addMapper();
      addMapperPage.fillSocialMapper("facebook attribute importer");
      masthead.checkNotificationMessage(createMapperSuccessMsg, true);
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
      cy.findByTestId("confirm").click();
    });

    it("clean up providers", () => {
      const modalUtils = new ModalUtils();

      // TODO: Re-enable this code when the 'should change order of providers' is no longer skipped.
      // sidebarPage.goToIdentityProviders();
      // listingPage.itemExist("bitbucket").deleteItem("bitbucket");
      // modalUtils.checkModalTitle(deletePrompt).confirmModal();
      // masthead.checkNotificationMessage(deleteSuccessMsg, true);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist("facebook").deleteItem("facebook");
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg, true);
    });
  });
});
