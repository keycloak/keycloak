import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import { keycloakBefore } from "../support/util/keycloak_before";
import AdminClient from "../support/util/AdminClient";

describe("Realm settings test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
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

    it("Go to general tab", function () {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.toggleSwitch(realmSettingsPage.managedAccessSwitch);
      realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
      realmSettingsPage.toggleSwitch(realmSettingsPage.managedAccessSwitch);
      realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    });

    it("Go to login tab", function () {
      sidebarPage.goToRealmSettings();
      cy.getId("rs-login-tab").click();
      realmSettingsPage.toggleSwitch(realmSettingsPage.userRegSwitch);
      realmSettingsPage.toggleSwitch(realmSettingsPage.forgotPwdSwitch);
      realmSettingsPage.toggleSwitch(realmSettingsPage.rememberMeSwitch);
      realmSettingsPage.toggleSwitch(realmSettingsPage.verifyEmailSwitch);
    });

    it("Go to email tab", function () {
      sidebarPage.goToRealmSettings();
      cy.getId("rs-email-tab").click();

      realmSettingsPage.addSenderEmail("example@example.com");

      cy.wait(100);

      realmSettingsPage.toggleCheck(realmSettingsPage.enableSslCheck);
      realmSettingsPage.toggleCheck(realmSettingsPage.enableStartTlsCheck);

      realmSettingsPage.save(realmSettingsPage.emailSaveBtn);
    });

    it("Go to themes tab", function () {
      sidebarPage.goToRealmSettings();
      cy.getId("rs-themes-tab").click();
      realmSettingsPage.selectLoginThemeType("keycloak");
      realmSettingsPage.selectAccountThemeType("keycloak");
      realmSettingsPage.selectAdminThemeType("base");
      realmSettingsPage.selectEmailThemeType("base");

      realmSettingsPage.saveThemes();
    });
  });
});
