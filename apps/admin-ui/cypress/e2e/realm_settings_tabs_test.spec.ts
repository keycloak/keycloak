import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import adminClient from "../support/util/AdminClient";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const masthead = new Masthead();
const realmSettingsPage = new RealmSettingsPage();

describe("Realm settings tabs tests", () => {
  const realmName = "Realm_" + (Math.random() + 1).toString(36).substring(7);

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
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
      false
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
    cy.findByTestId("rs-login-tab").click();
  }

  function testToggle(realmSwitch: string, expectedValue: string) {
    realmSettingsPage.toggleSwitch(realmSwitch);
    reloadRealm();
    cy.findByTestId(realmSwitch).should("have.value", expectedValue);
  }

  it("Go to login tab", () => {
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-login-tab").click();

    testToggle(realmSettingsPage.userRegSwitch, "on");
    testToggle(realmSettingsPage.forgotPwdSwitch, "on");
    testToggle(realmSettingsPage.rememberMeSwitch, "on");
    testToggle(realmSettingsPage.loginWithEmailSwitch, "off");
    testToggle(realmSettingsPage.duplicateEmailsSwitch, "on");

    // Check other values
    cy.findByTestId(realmSettingsPage.emailAsUsernameSwitch).should(
      "have.value",
      "off"
    );

    cy.findByTestId(realmSettingsPage.verifyEmailSwitch).should(
      "have.value",
      "off"
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
    cy.findByTestId("rs-email-tab").click();
    //required fields not filled in or not filled properly
    realmSettingsPage.addSenderEmail("not a valid email");
    realmSettingsPage.fillFromDisplayName("displayName");
    realmSettingsPage.fillReplyToEmail("replyTo@email.com");
    realmSettingsPage.fillPort("10");
    cy.findByTestId("email-tab-save").click();
    cy.get("#kc-display-name-helper").contains("You must enter a valid email.");
    cy.get("#kc-host-helper").contains("Required field");
    //revert
    cy.wait(100);
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
    realmSettingsPage.selectAdminThemeType("base");
    realmSettingsPage.selectEmailThemeType("base");

    realmSettingsPage.saveThemes();
  });
});
