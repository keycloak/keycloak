import { v4 as uuid } from "uuid";

import FormValidation from "../support/forms/FormValidation";
import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import KeysTab from "../support/pages/admin-ui/manage/realm_settings/KeysTab";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import UserRegistration from "../support/pages/admin-ui/manage/realm_settings/UserRegistration";
import adminClient from "../support/util/AdminClient";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const masthead = new Masthead();
const realmSettingsPage = new RealmSettingsPage();
const userRegistration = new UserRegistration();
const keysTab = new KeysTab();
const modalUtils = new ModalUtils();

describe("Realm settings tabs tests", () => {
  const realmName = "Realm_" + uuid();

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealm(realmName);
  });

  before(async () => {
    await adminClient.createRealm(realmName);
  });

  after(async () => {
    await adminClient.deleteRealm(realmName);
  });

  const addBundle = () => {
    realmSettingsPage.addKeyValuePair("123", "abc");

    return this;
  };

  it("shows the 'user profile' tab", () => {
    sidebarPage.goToRealmSettings();
    cy.findByTestId(realmSettingsPage.userProfileTab).should("exist");
  });

  // Clicking multiple toggles in succession causes quick re-renderings of the screen
  // and there will be a noticeable flicker during the test.
  // Sometimes, this will screw up the test and cause Cypress to hang.
  // Clicking to another section each time fixes the problem.
  function reloadRealm() {
    sidebarPage.goToClientScopes();
    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToLoginTab();
  }

  function testToggle(realmSwitch: string, expectedValue: string) {
    realmSettingsPage.toggleSwitch(realmSwitch);
    reloadRealm();
    cy.findByTestId(realmSwitch).should("have.value", expectedValue);
  }

  it("Go to login tab", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToLoginTab();

    testToggle(realmSettingsPage.userRegSwitch, "on");
    testToggle(realmSettingsPage.forgotPwdSwitch, "on");
    testToggle(realmSettingsPage.rememberMeSwitch, "on");
    testToggle(realmSettingsPage.loginWithEmailSwitch, "off");
    testToggle(realmSettingsPage.duplicateEmailsSwitch, "on");

    // Check other values
    cy.findByTestId(realmSettingsPage.emailAsUsernameSwitch).should(
      "have.value",
      "off",
    );

    cy.findByTestId(realmSettingsPage.verifyEmailSwitch).should(
      "have.value",
      "off",
    );
  });

  it("Go to email tab", () => {
    // Configure an e-mail address so we can test the connection settings.
    cy.wrap(null).then(async () => {
      const adminUser = await adminClient.getAdminUser();

      await adminClient.updateUser(adminUser.id!, {
        email: "admin@example.com",
      });
    });

    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToEmailTab();
    //required fields not filled in or not filled properly
    realmSettingsPage.addSenderEmail("not a valid email");
    realmSettingsPage.fillFromDisplayName("displayName");
    realmSettingsPage.fillReplyToEmail("replyTo@email.com");
    realmSettingsPage.fillPort("10");
    cy.findByTestId("email-tab-save").click();

    FormValidation.assertMessage(
      realmSettingsPage.getFromInput(),
      "You must enter a valid email.",
    );
    FormValidation.assertRequired(realmSettingsPage.getHostInput());

    cy.findByTestId("email-tab-revert").click();
    cy.findByTestId("smtpServer.from").should("be.empty");
    cy.findByTestId("smtpServer.fromDisplayName").should("be.empty");
    cy.findByTestId("smtpServer.port").should("be.empty");

    realmSettingsPage.addSenderEmail("example@example.com");
    realmSettingsPage.toggleCheck(realmSettingsPage.enableSslCheck);
    realmSettingsPage.toggleCheck(realmSettingsPage.enableStartTlsCheck);
    realmSettingsPage.fillHostField("localhost");

    cy.findByTestId(realmSettingsPage.testConnectionButton).click();

    masthead.checkNotificationMessage("Error! Failed to send email", true);
  });

  it("Go to themes tab", () => {
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-themes-tab").click();

    realmSettingsPage.selectLoginThemeType("keycloak");
    realmSettingsPage.selectAccountThemeType("keycloak");
    realmSettingsPage.selectEmailThemeType("base");

    realmSettingsPage.saveThemes();
  });

  describe("Go to security defenses tab", () => {
    it("Realm header settings- update single input", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToSecurityDefensesTab();
      cy.findByTestId("browserSecurityHeaders.xFrameOptions").clear();
      cy.findByTestId("browserSecurityHeaders.xFrameOptions").type("DENY");
      realmSettingsPage.saveSecurityDefensesHeaders();
      masthead.checkNotificationMessage("Realm successfully updated");
    });

    it("Realm header settings- update all inputs", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToSecurityDefensesTab();
      cy.findByTestId("browserSecurityHeaders.xFrameOptions").clear();
      cy.findByTestId("browserSecurityHeaders.xFrameOptions").type(
        "SAMEORIGIN",
      );
      cy.findByTestId("browserSecurityHeaders.contentSecurityPolicy").clear();
      cy.findByTestId("browserSecurityHeaders.contentSecurityPolicy").type(
        "default-src 'self'",
      );
      cy.findByTestId("browserSecurityHeaders.strictTransportSecurity").clear();
      cy.findByTestId("browserSecurityHeaders.strictTransportSecurity").type(
        "max-age=31536000",
      );
      cy.findByTestId("browserSecurityHeaders.xContentTypeOptions").clear();
      cy.findByTestId("browserSecurityHeaders.xContentTypeOptions").type(
        "nosniff",
      );
      cy.findByTestId("browserSecurityHeaders.xRobotsTag").clear();
      cy.findByTestId("browserSecurityHeaders.xRobotsTag").type("none");
      cy.findByTestId("browserSecurityHeaders.xXSSProtection").clear();
      cy.findByTestId("browserSecurityHeaders.xXSSProtection").type(
        "1; mode=block",
      );
      cy.findByTestId("browserSecurityHeaders.strictTransportSecurity").clear();
      cy.findByTestId("browserSecurityHeaders.strictTransportSecurity").type(
        "max-age=31537000",
      );
      cy.findByTestId("browserSecurityHeaders.referrerPolicy").clear();
      cy.findByTestId("browserSecurityHeaders.referrerPolicy").type("referrer");
      realmSettingsPage.saveSecurityDefensesHeaders();
      masthead.checkNotificationMessage("Realm successfully updated");
    });

    it("Brute force detection- update values", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToSecurityDefensesTab();
      realmSettingsPage.goToSecurityDefensesBruteForceTab();
      cy.get("#kc-brute-force-mode").click();
      cy.findByTestId("select-brute-force-mode")
        .contains("Lockout temporarily")
        .click();
      cy.findByTestId("waitIncrementSeconds").type("1");
      cy.findByTestId("maxFailureWaitSeconds").type("1");
      cy.findByTestId("maxDeltaTimeSeconds").type("1");
      cy.findByTestId("minimumQuickLoginWaitSeconds").type("1");
      realmSettingsPage.saveSecurityDefensesBruteForce();
      masthead.checkNotificationMessage("Realm successfully updated");
    });
  });

  describe("Go to localization tab", () => {
    it("Locales tab - Add locale", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationLocalesSubTab();

      cy.findByTestId("internationalizationEnabled").click({ force: true });

      cy.get(realmSettingsPage.supportedLocalesTypeahead)
        .click()
        .get(".pf-v5-c-menu__list-item")
        .contains("Danish")
        .click({ force: true });
      cy.findByTestId("internationalizationEnabled").click({ force: true });

      cy.intercept("GET", `/admin/realms/${realmName}/localization/en*`).as(
        "load",
      );

      cy.findByTestId("localization-tab-save").click();
      cy.wait("@load");

      masthead.checkNotificationMessage("Realm successfully updated");
    });

    it("Realm Overrides - Add and delete bundle", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationRealmOverridesSubTab();

      addBundle();

      masthead.checkNotificationMessage(
        "Success! The translation has been added.",
      );

      cy.findByTestId("editable-rows-table")
        .contains("td", "123")
        .should("be.visible");

      cy.get(".pf-v5-c-table__action button").click();
      cy.contains("button", "Delete").click();
      cy.findByTestId("confirm").click();
      masthead.checkNotificationMessage("Successfully removed translation(s).");
    });

    it("Realm Overrides - Search for and delete bundle", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationRealmOverridesSubTab();

      addBundle();

      cy.get('input[aria-label="Search"]').type("123");

      cy.findByTestId("editable-rows-table")
        .contains("td", "123")
        .should("be.visible");

      cy.findByTestId("selectAll").click();
      cy.get('[data-testid="toolbar-deleteBtn"]').click();
      cy.findByTestId("delete-selected-TranslationBtn").click();
      cy.findByTestId("confirm").click();
      masthead.checkNotificationMessage("Successfully removed translation(s).");
    });

    it("Realm Overrides - Edit and cancel edit message bundle", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationRealmOverridesSubTab();

      addBundle();

      cy.findByTestId("editTranslationBtn-0").click();
      cy.findByTestId("editTranslationCancelBtn-0").click();

      cy.findByTestId("editTranslationBtn-0").click();
      cy.findByTestId("editTranslationValueInput-0")
        .click()
        .clear()
        .type("def");
      cy.findByTestId("editTranslationAcceptBtn-0").click();

      cy.findByTestId("editable-rows-table")
        .contains("td", "def")
        .should("be.visible");

      cy.get(".pf-v5-c-table__action button").click();
      cy.contains("button", "Delete").click();
      cy.findByTestId("confirm").click();

      masthead.checkNotificationMessage("Successfully removed translation(s).");
    });

    it("Effective Message Bundles - Check before search message", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationEffectiveMessageBundlesSubTab();
      cy.contains("h1", "Search for effective messages");
    });
  });

  describe("Accessibility tests for realm settings", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToRealmSettings();
      cy.injectAxe();
    });

    it("Check a11y violations on load/ realm settings/ general tab", () => {
      cy.checkA11y();
    });

    it("Check a11y violations on login tab", () => {
      realmSettingsPage.goToLoginTab();
      cy.checkA11y();
    });

    it("Check a11y violations on email tab", () => {
      realmSettingsPage.goToEmailTab();
      cy.checkA11y();
    });

    it("Check a11y violations on themes tab", () => {
      realmSettingsPage.goToThemesTab();
      cy.checkA11y();
    });

    it("Check a11y violations on keys tab/ keys list sub tab", () => {
      keysTab.goToKeysTab();
      cy.checkA11y();
    });

    it("Check a11y violations on keys tab/ providers sub tab", () => {
      keysTab.goToProvidersTab();
      cy.checkA11y();
    });

    it("Check a11y violations on keys tab/ providers sub tab / adding provider", () => {
      keysTab.goToProvidersTab();
      cy.findByTestId("addProviderDropdown").click();
      cy.checkA11y();
      modalUtils.closeModal();
    });

    it("Check a11y violations on events tab/ event listeners sub tab", () => {
      realmSettingsPage.goToEventsTab();
      cy.checkA11y();
    });

    it("Check a11y violations on events tab/ user events settings sub tab", () => {
      realmSettingsPage.goToEventsTab().goToUserEventsSettingsSubTab();
      cy.checkA11y();
    });

    it("Check a11y violations on events tab/ admin events settings sub tab", () => {
      realmSettingsPage.goToEventsTab().goToAdminEventsSettingsSubTab();
      cy.checkA11y();
    });

    it("Check a11y violations on localization locales sub tab", () => {
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationLocalesSubTab();
      cy.checkA11y();
    });

    it("Check a11y violations on localization realm overrides sub tab", () => {
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationRealmOverridesSubTab();
      cy.checkA11y();
    });

    it("Check a11y violations on localization realm overrides sub tab/ adding message bundle", () => {
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationRealmOverridesSubTab();
      cy.findByTestId("add-translationBtn").click();
      cy.checkA11y();
      modalUtils.cancelModal();
    });

    it("Check a11y violations on localization effective message bundles sub tab", () => {
      realmSettingsPage.goToLocalizationTab();
      realmSettingsPage.goToLocalizationEffectiveMessageBundlesSubTab();
      cy.checkA11y();
    });

    it("Check a11y violations on security defenses tab", () => {
      realmSettingsPage.goToSecurityDefensesTab();
      cy.checkA11y();
    });

    it("Check a11y violations on sessions tab", () => {
      realmSettingsPage.goToSessionsTab();
      cy.checkA11y();
    });

    it("Check a11y violations on tokens tab", () => {
      realmSettingsPage.goToTokensTab();
      cy.checkA11y();
    });

    it("Check a11y violations on client policies tab/ profiles sub tab", () => {
      realmSettingsPage.goToClientPoliciesTab().goToClientProfilesList();
      cy.checkA11y();
    });

    it("Check a11y violations on client policies tab/ creating profile", () => {
      realmSettingsPage.goToClientPoliciesTab().goToClientProfilesList();
      cy.findByTestId("createProfile").click();
      cy.checkA11y();
      cy.findByTestId("cancelCreateProfile").click();
    });

    it("Check a11y violations on client policies tab/ policies sub tab", () => {
      realmSettingsPage.goToClientPoliciesTab().goToClientPoliciesList();
      cy.checkA11y();
    });

    it("Check a11y violations on client policies tab/ creating policy", () => {
      realmSettingsPage.goToClientPoliciesTab().goToClientPoliciesList();
      cy.findByTestId("no-client-policies-empty-action").click();
      cy.checkA11y();
      cy.findByTestId("cancelCreatePolicy").click();
    });

    it("Check a11y violations on user registration tab/ default roles sub tab", () => {
      userRegistration.goToTab();
      cy.checkA11y();
    });

    it("Check a11y violations on user registration tab/ default roles sub tab/ assigning role", () => {
      userRegistration.goToTab();
      cy.findByTestId("assignRole").click();
      cy.checkA11y();
      modalUtils.cancelModal();
    });
  });
});
