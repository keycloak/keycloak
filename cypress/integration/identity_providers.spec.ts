import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_before";
import ListingPage from "../support/pages/admin_console/ListingPage";

import CreateProviderPage from "../support/pages/admin_console/manage/identity_providers/CreateProviderPage";
import ModalUtils from "../support/util/ModalUtils";
import OrderDialog from "../support/pages/admin_console/manage/identity_providers/OrderDialog";

describe("Identity provider test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const masthead = new Masthead();
  const listingPage = new ListingPage();
  const createProviderPage = new CreateProviderPage();

  describe("Identity provider creation", () => {
    const identityProviderName = "github";

    beforeEach(function () {
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
      masthead.checkNotificationMessage(
        "Identity provider successfully created"
      );

      //TODO temporary refresh
      sidebarPage.goToAuthentication().goToIdentityProviders();

      listingPage.itemExist(identityProviderName);
    });

    it("should delete provider", () => {
      const modalUtils = new ModalUtils();
      listingPage.deleteItem(identityProviderName);
      modalUtils.checkModalTitle("Delete provider?").confirmModal();

      masthead.checkNotificationMessage("Provider successfully deleted");

      createProviderPage.checkGitHubCardVisible();
    });

    it("should change order of providers", () => {
      const orderDialog = new OrderDialog();
      const providers = ["facebook", identityProviderName, "bitbucket"];

      createProviderPage
        .clickCard("facebook")
        .fill("facebook", "123")
        .clickAdd();
      sidebarPage.goToIdentityProviders();
      listingPage.itemExist("facebook");

      createProviderPage
        .clickCreateDropdown()
        .clickItem(identityProviderName)
        .fill(identityProviderName, "123")
        .clickAdd();
      sidebarPage.goToIdentityProviders();
      createProviderPage
        .clickCreateDropdown()
        .clickItem("bitbucket")
        .fill("bitbucket", "123")
        .clickAdd();
      sidebarPage.goToIdentityProviders();

      orderDialog.openDialog().checkOrder(providers);
      orderDialog.moveRowTo("facebook", identityProviderName);

      orderDialog.checkOrder(["facebook", "bitbucket", identityProviderName]);

      orderDialog.clickSave();
      masthead.checkNotificationMessage(
        "Successfully changed display order of identity providers"
      );
    });
  });
});
