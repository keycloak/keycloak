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

      sidebarPage.goToIdentityProviders();
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

      cy.wait(2000);
      sidebarPage.goToIdentityProviders();
      listingPage.itemExist("facebook");

      createProviderPage
        .clickCreateDropdown()
        .clickItem(identityProviderName)
        .fill(identityProviderName, "123")
        .clickAdd();

      cy.wait(2000);
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

      orderDialog.checkOrder(["facebook", "bitbucket", identityProviderName]);

      orderDialog.clickSave();
      masthead.checkNotificationMessage(
        "Successfully changed display order of identity providers"
      );
    });

    it("should create a oidc provider using discovery url", () => {
      const oidcProviderName = "oidc";
      const keycloakServer = Cypress.env("KEYCLOAK_SERVER");

      createProviderPage
        .clickCreateDropdown()
        .clickItem(oidcProviderName)
        .fillDiscoveryUrl(
          `${keycloakServer}/auth/realms/master/.well-known/openid-configuration`
        )
        .shouldBeSuccessful()
        .fill("oidc", "123")
        .clickAdd();
      masthead.checkNotificationMessage(
        "Identity provider successfully created"
      );

      createProviderPage.shouldHaveAuthorizationUrl(
        `${keycloakServer}/auth/realms/master/protocol/openid-connect/auth`
      );
    });

    // it("clean up providers", () => {
    //   const modalUtils = new ModalUtils();
    //   listingPage.deleteItem("bitbucket");
    //   modalUtils.checkModalTitle("Delete provider?").confirmModal();
    //   masthead.checkNotificationMessage("Provider successfully deleted");

    //   listingPage.deleteItem("facebook");
    //   modalUtils.checkModalTitle("Delete provider?").confirmModal();
    //   masthead.checkNotificationMessage("Provider successfully deleted");

    //   listingPage.deleteItem("github");
    //   modalUtils.checkModalTitle("Delete provider?").confirmModal();
    //   masthead.checkNotificationMessage("Provider successfully deleted");

    //   listingPage.deleteItem("oidc");
    //   modalUtils.checkModalTitle("Delete provider?").confirmModal();
    //   masthead.checkNotificationMessage("Provider successfully deleted");
    // });
  });
});
