import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_before";
import AdminClient from "../support/util/AdminClient";
import ListingPage from "../support/pages/admin_console/ListingPage";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const masthead = new Masthead();
const modalUtils = new ModalUtils();
const realmSettingsPage = new RealmSettingsPage();

describe("Realm settings", () => {
  const realmName = "Realm_" + (Math.random() + 1).toString(36).substring(7);

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToRealm(realmName);
  });

  before(async () => {
    await new AdminClient().createRealm(realmName);
  });

  after(async () => {
    await new AdminClient().deleteRealm(realmName);
  });

  const goToKeys = () => {
    const keysUrl = "/auth/admin/realms/master/keys";
    cy.intercept(keysUrl).as("keysFetch");
    cy.getId("rs-keys-tab").click();
    cy.wait(10000);
    cy.getId("rs-keys-list-tab").click();

    return this;
  };

  const addBundle = () => {
    const localizationUrl =
      "/auth/admin/realms/master/realm-settings/localization";
    cy.intercept(localizationUrl).as("localizationFetch");

    realmSettingsPage.addKeyValuePair(
      "key_" + (Math.random() + 1).toString(36).substring(7),
      "value_" + (Math.random() + 1).toString(36).substring(7)
    );

    return this;
  };

  it("Go to general tab", function () {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.toggleSwitch(realmSettingsPage.managedAccessSwitch);
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    realmSettingsPage.toggleSwitch(realmSettingsPage.managedAccessSwitch);
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
  });

  it("Go to login tab", () => {
    sidebarPage.goToRealmSettings();
    cy.getId("rs-login-tab").click();
    realmSettingsPage.toggleSwitch(realmSettingsPage.userRegSwitch);
    realmSettingsPage.toggleSwitch(realmSettingsPage.forgotPwdSwitch);
    realmSettingsPage.toggleSwitch(realmSettingsPage.rememberMeSwitch);
    realmSettingsPage.toggleSwitch(realmSettingsPage.verifyEmailSwitch);
  });

  it("Go to email tab", () => {
    sidebarPage.goToRealmSettings();
    cy.getId("rs-email-tab").click();

    cy.wait(1000);

    realmSettingsPage.addSenderEmail("example@example.com");

    realmSettingsPage.toggleCheck(realmSettingsPage.enableSslCheck);
    realmSettingsPage.toggleCheck(realmSettingsPage.enableStartTlsCheck);

    realmSettingsPage.save(realmSettingsPage.emailSaveBtn);

    realmSettingsPage.fillHostField("localhost");
    cy.getId(realmSettingsPage.testConnectionButton).click();

    realmSettingsPage.fillEmailField(
      "example" + (Math.random() + 1).toString(36).substring(7) + "@example.com"
    );

    cy.getId(realmSettingsPage.modalTestConnectionButton).click();

    masthead.checkNotificationMessage("Error! Failed to send email.");
  });

  it("Go to themes tab", () => {
    cy.wait(5000);
    sidebarPage.goToRealmSettings();
    cy.getId("rs-themes-tab").click();
    realmSettingsPage.selectLoginThemeType("keycloak");
    realmSettingsPage.selectAccountThemeType("keycloak");
    realmSettingsPage.selectAdminThemeType("base");
    realmSettingsPage.selectEmailThemeType("base");

    realmSettingsPage.saveThemes();
  });

  describe("Events tab", () => {
    const listingPage = new ListingPage();

    it("Enable user events", () => {
      sidebarPage.goToRealmSettings();
      cy.getId("rs-realm-events-tab").click();

      cy.wait(5000);
      realmSettingsPage
        .toggleSwitch(realmSettingsPage.enableEvents)
        .save(realmSettingsPage.eventsUserSave);
      masthead.checkNotificationMessage("Successfully saved configuration");

      realmSettingsPage.clearEvents("user");

      modalUtils
        .checkModalMessage(
          "If you clear all events of this realm, all records will be permanently cleared in the database"
        )
        .confirmModal();

      masthead.checkNotificationMessage("The user events have been cleared");

      const events = ["Client info", "Client info error"];

      cy.intercept("GET", `/auth/admin/realms/${realmName}/events/config`).as(
        "fetchConfig"
      );
      realmSettingsPage.addUserEvents(events).clickAdd();
      masthead.checkNotificationMessage("Successfully saved configuration");
      cy.wait(["@fetchConfig"]);
      cy.get(".pf-c-spinner__tail-ball").should("not.exist");

      for (const event of events) {
        listingPage.searchItem(event, false).itemExist(event);
      }
    });
  });

  it("Go to keys tab", () => {
    cy.wait(5000);

    sidebarPage.goToRealmSettings();

    cy.getId("rs-keys-tab").click();
  });

  it("add Providers", () => {
    sidebarPage.goToRealmSettings();

    cy.getId("rs-keys-tab").click();

    cy.getId("rs-providers-tab").click();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.getId("option-aes-generated").click();
    realmSettingsPage.enterConsoleDisplayName("test_aes-generated");
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.getId("option-ecdsa-generated").click();
    realmSettingsPage.enterConsoleDisplayName("test_ecdsa-generated");
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.getId("option-hmac-generated").click();
    realmSettingsPage.enterConsoleDisplayName("test_hmac-generated");
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.getId("option-rsa-generated").click();
    realmSettingsPage.enterConsoleDisplayName("test_rsa-generated");
    realmSettingsPage.addProvider();
  });

  it("Test keys", () => {
    sidebarPage.goToRealmSettings();
    goToKeys();

    realmSettingsPage.testSelectFilter();
  });

  it("add locale", () => {
    sidebarPage.goToRealmSettings();

    cy.getId("rs-localization-tab").click();

    addBundle();

    masthead.checkNotificationMessage(
      "Success! The localization text has been created."
    );
  });
});
