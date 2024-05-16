import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import ListingPage from "../support/pages/admin-ui/ListingPage";

import CreateProviderPage from "../support/pages/admin-ui/manage/identity_providers/CreateProviderPage";
import ModalUtils from "../support/util/ModalUtils";
import OrderDialog from "../support/pages/admin-ui/manage/identity_providers/OrderDialog";
import AddMapperPage from "../support/pages/admin-ui/manage/identity_providers/AddMapperPage";
import ProviderFacebookGeneralSettings from "../support/pages/admin-ui/manage/identity_providers/social/ProviderFacebookGeneralSettings";
import ProviderBaseGeneralSettingsPage from "../support/pages/admin-ui/manage/identity_providers/ProviderBaseGeneralSettingsPage";
import ProviderBaseAdvancedSettingsPage from "../support/pages/admin-ui/manage/identity_providers/ProviderBaseAdvancedSettingsPage";
import ProviderGithubGeneralSettings from "../support/pages/admin-ui/manage/identity_providers/social/ProviderGithubGeneralSettings";
import ProviderGoogleGeneralSettings from "../support/pages/admin-ui/manage/identity_providers/social/ProviderGoogleGeneralSettings";
import ProviderMicrosoftGeneralSettings from "../support/pages/admin-ui/manage/identity_providers/social/ProviderMicrosoftGeneralSettings";
import ProviderOpenshiftGeneralSettings from "../support/pages/admin-ui/manage/identity_providers/social/ProviderOpenshiftGeneralSettings";
import ProviderPaypalGeneralSettings from "../support/pages/admin-ui/manage/identity_providers/social/ProviderPaypalGeneralSettings";
import ProviderStackoverflowGeneralSettings from "../support/pages/admin-ui/manage/identity_providers/social/ProviderStackoverflowGeneralSettings";
import adminClient from "../support/util/AdminClient";
import GroupPage from "../support/pages/admin-ui/manage/groups/GroupPage";
import CommonPage from "../support/pages/CommonPage";

describe("Identity provider test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const masthead = new Masthead();
  const listingPage = new ListingPage();
  const createProviderPage = new CreateProviderPage();
  const addMapperPage = new AddMapperPage();
  const groupPage = new GroupPage();
  const commonPage = new CommonPage();

  const createSuccessMsg = "Identity provider successfully created";
  const createFailMsg =
    "Could not create the identity provider: Identity Provider github already exists";
  const createMapperSuccessMsg = "Mapper created successfully.";

  const changeSuccessMsg =
    "Successfully changed display order of identity providers";
  const deletePrompt = "Delete provider?";
  const deleteSuccessMsg = "Provider successfully deleted.";

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToIdentityProviders();
  });

  const socialLoginIdentityProvidersWithCustomFiels = {
    Facebook: new ProviderFacebookGeneralSettings(),
    Github: new ProviderGithubGeneralSettings(),
    Google: new ProviderGoogleGeneralSettings(),
    Microsoft: new ProviderMicrosoftGeneralSettings(),
    "Openshift-v3": new ProviderOpenshiftGeneralSettings(),
    "Openshift-v4": new ProviderOpenshiftGeneralSettings(),
    Paypal: new ProviderPaypalGeneralSettings(),
    Stackoverflow: new ProviderStackoverflowGeneralSettings(),
  };
  function getSocialIdpClassInstance(idpTestName: string) {
    let instance = new ProviderBaseGeneralSettingsPage();
    Object.entries(socialLoginIdentityProvidersWithCustomFiels).find(
      ([key, value]) => {
        if (key === idpTestName) {
          instance = value;
          return true;
        }
        return false;
      },
    );
    return instance;
  }

  describe("Identity provider creation", () => {
    const identityProviderName = "github";

    describe("Custom fields tests", () => {
      const socialLoginIdentityProviders = [
        { testName: "Bitbucket", displayName: "BitBucket", alias: "bitbucket" },
        { testName: "Facebook", displayName: "Facebook", alias: "facebook" },
        { testName: "Github", displayName: "GitHub", alias: "github" },
        { testName: "Gitlab", displayName: "Gitlab", alias: "gitlab" },
        { testName: "Google", displayName: "Google", alias: "google" },
        { testName: "Instagram", displayName: "Instagram", alias: "instagram" },
        {
          testName: "LinkedIn",
          displayName: "LinkedIn",
          alias: "linkedin-openid-connect",
        },
        { testName: "Microsoft", displayName: "Microsoft", alias: "microsoft" },
        {
          testName: "Openshift-v3",
          displayName: "Openshift v3",
          alias: "openshift-v3",
        },
        {
          testName: "Openshift-v4",
          displayName: "Openshift v4",
          alias: "openshift-v4",
        },
        { testName: "Paypal", displayName: "PayPal", alias: "paypal" },
        {
          testName: "Stackoverflow",
          displayName: "StackOverflow",
          alias: "stackoverflow",
        },
        { testName: "Twitter", displayName: "Twitter", alias: "twitter" },
      ];

      after(async () => {
        await Promise.all(
          socialLoginIdentityProviders.map((idp) =>
            adminClient.deleteIdentityProvider(idp.alias),
          ),
        );
      });

      socialLoginIdentityProviders.forEach(($idp, linkedIdpsCount) => {
        it(`should create social login provider ${$idp.testName} with custom fields`, () => {
          if (linkedIdpsCount == 0) {
            createProviderPage.clickCard($idp.alias);
          } else {
            createProviderPage.clickCreateDropdown().clickItem($idp.alias);
          }
          const instance = getSocialIdpClassInstance($idp.testName);
          instance
            .typeClientId("1")
            .typeClientId("")
            .typeClientSecret("1")
            .typeClientSecret("")
            .assertRequiredFieldsErrorsExist()
            .fillData($idp.testName)
            .clickAdd()
            .assertNotificationIdpCreated()
            .assertFilledDataEqual($idp.testName);
        });
      });
    });

    it("should create github provider", () => {
      createProviderPage.checkGitHubCardVisible().clickGitHubCard();

      createProviderPage.checkAddButtonDisabled();
      createProviderPage.fill(identityProviderName).checkAddButtonDisabled();
      createProviderPage.fill(identityProviderName, "123").clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(identityProviderName);
    });

    it("fail to make duplicate github provider", () => {
      createProviderPage
        .clickCreateDropdown()
        .clickItem("github")
        .fill("github2", "123")
        .clickAdd();
      masthead.checkNotificationMessage(createFailMsg, true);
    });

    it("should create facebook provider", () => {
      createProviderPage
        .clickCreateDropdown()
        .clickItem("facebook")
        .fill("facebook", "123")
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);
    });

    it("search for existing provider by name", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.searchItem(identityProviderName, false);
      listingPage.itemExist(identityProviderName, true);
    });

    it("search for non-existing provider by name", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.searchItem("not-existing-provider", false);
      groupPage.assertNoSearchResultsMessageExist(true);
    });

    it("create and delete provider by item details", () => {
      createProviderPage
        .clickCreateDropdown()
        .clickItem("linkedin-openid-connect")
        .fill("linkedin-openid-connect", "123")
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);

      commonPage
        .actionToolbarUtils()
        .clickActionToggleButton()
        .clickDropdownItem("Delete");

      const modalUtils = new ModalUtils();
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg, true);
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

  describe("should check provider details", () => {
    const identityProviderName = "github";
    const githubSettings = new ProviderGithubGeneralSettings();
    const advancedSettings = new ProviderBaseAdvancedSettingsPage();

    it("creating github provider", () => {
      createProviderPage.checkGitHubCardVisible().clickGitHubCard();

      createProviderPage.checkAddButtonDisabled();
      createProviderPage
        .fill(identityProviderName)
        .fill(identityProviderName, "123")
        .clickAdd();
      masthead.checkNotificationMessage(createSuccessMsg, true);

      sidebarPage.goToIdentityProviders();
      listingPage.itemExist(identityProviderName);
    });

    it("should check general settings", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails("github");

      githubSettings.fillData("github");
      cy.findByTestId("idp-details-save").click();
    });

    it("should check input switches and inputs", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails("github");

      advancedSettings.typeScopesInput("openid");
      advancedSettings.assertScopesInputEqual("openid");

      advancedSettings.assertStoreTokensSwitchTurnedOn(false);
      advancedSettings.assertAcceptsPromptNoneForwardFromClientSwitchTurnedOn(
        false,
      );
      advancedSettings.assertDisableUserInfoSwitchTurnedOn(false);
      advancedSettings.assertTrustEmailSwitchTurnedOn(false);
      advancedSettings.assertAccountLinkingOnlySwitchTurnedOn(false);
      advancedSettings.assertHideOnLoginPageSwitchTurnedOn(false);

      advancedSettings.clickStoreTokensSwitch();
      advancedSettings.clickAcceptsPromptNoneForwardFromClientSwitch();
      advancedSettings.clickDisableUserInfoSwitch();
      advancedSettings.clickTrustEmailSwitch();
      advancedSettings.clickAccountLinkingOnlySwitch();
      advancedSettings.clickHideOnLoginPageSwitch();
      advancedSettings.assertDoNotImportUsersSwitchTurnedOn(false);
      advancedSettings.assertSyncModeShown(true);
      advancedSettings.clickdoNotStoreUsersSwitch();
      advancedSettings.assertDoNotImportUsersSwitchTurnedOn(true);
      advancedSettings.assertSyncModeShown(false);
      advancedSettings.clickdoNotStoreUsersSwitch();
      advancedSettings.assertDoNotImportUsersSwitchTurnedOn(false);
      advancedSettings.assertSyncModeShown(true);

      advancedSettings.clickEssentialClaimSwitch();
      advancedSettings.typeClaimNameInput("claim-name");
      advancedSettings.typeClaimValueInput("claim-value");

      advancedSettings.ensureAdvancedSettingsAreVisible();
      advancedSettings.assertStoreTokensSwitchTurnedOn(true);
      advancedSettings.assertAcceptsPromptNoneForwardFromClientSwitchTurnedOn(
        true,
      );
      advancedSettings.assertDisableUserInfoSwitchTurnedOn(true);
      advancedSettings.assertTrustEmailSwitchTurnedOn(true);
      advancedSettings.assertAccountLinkingOnlySwitchTurnedOn(true);
      advancedSettings.assertHideOnLoginPageSwitchTurnedOn(true);
      advancedSettings.assertEssentialClaimSwitchTurnedOn(true);
      advancedSettings.assertClaimInputEqual("claim-name");
      advancedSettings.assertClaimValueInputEqual("claim-value");

      cy.findByTestId("idp-details-save").click();
      masthead.checkNotificationMessage("Provider successfully updated");
    });

    it("should revert and save options", () => {
      sidebarPage.goToIdentityProviders();
      listingPage.goToItemDetails("github");

      cy.findByTestId("jump-link-advanced-settings").click();
      advancedSettings.assertStoreTokensSwitchTurnedOn(true);
      advancedSettings.assertAcceptsPromptNoneForwardFromClientSwitchTurnedOn(
        true,
      );
      advancedSettings.clickStoreTokensSwitch();
      advancedSettings.clickAcceptsPromptNoneForwardFromClientSwitch();
      advancedSettings.ensureAdvancedSettingsAreVisible();
      advancedSettings.assertStoreTokensSwitchTurnedOn(false);
      advancedSettings.assertAcceptsPromptNoneForwardFromClientSwitchTurnedOn(
        false,
      );
      cy.findByTestId("idp-details-revert").click();
      advancedSettings.assertStoreTokensSwitchTurnedOn(true);
      advancedSettings.assertAcceptsPromptNoneForwardFromClientSwitchTurnedOn(
        true,
      );
    });

    it("should delete providers", () => {
      const modalUtils = new ModalUtils();
      sidebarPage.goToIdentityProviders();

      listingPage.itemExist("github").deleteItem("github");
      modalUtils.checkModalTitle(deletePrompt).confirmModal();
      masthead.checkNotificationMessage(deleteSuccessMsg, true);
    });
  });

  describe("Accessibility tests for identity providers", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToIdentityProviders();
      cy.injectAxe();
    });

    it("Check a11y violations on load/ identity providers", () => {
      cy.checkA11y();
    });
  });
});
