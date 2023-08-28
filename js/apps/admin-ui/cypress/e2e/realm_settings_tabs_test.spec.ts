import { v4 as uuid } from "uuid";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import adminClient from "../support/util/AdminClient";
import KeysTab from "../support/pages/admin-ui/manage/realm_settings/KeysTab";
import ModalUtils from "../support/util/ModalUtils";
import UserRegistration from "../support/pages/admin-ui/manage/realm_settings/UserRegistration";

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

  it("shows the 'user profile' tab if enabled", () => {
    sidebarPage.goToRealmSettings();
    cy.findByTestId(realmSettingsPage.userProfileTab).should("not.exist");
    realmSettingsPage.toggleSwitch(
      realmSettingsPage.profileEnabledSwitch,
      false,
    );
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    masthead.checkNotificationMessage("Realm successfully updated");
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
    cy.get("#kc-display-name-helper").contains("You must enter a valid email.");
    cy.get("#kc-host-helper").contains("Required field");

    cy.findByTestId("email-tab-revert").click();
    cy.findByTestId("sender-email-address").should("be.empty");
    cy.findByTestId("from-display-name").should("be.empty");
    cy.get("#kc-port").should("be.empty");

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

    it("Check a11y violations on localization tab", () => {
      realmSettingsPage.goToLocalizationTab();
      cy.checkA11y();
    });

    it("Check a11y violations on localization tab/ adding message bundle", () => {
      realmSettingsPage.goToLocalizationTab();
      cy.findByTestId("add-bundle-button").click();
      cy.checkA11y();
      modalUtils.cancelModal();
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
