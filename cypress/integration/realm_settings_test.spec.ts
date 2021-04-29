import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import { keycloakBefore } from "../support/util/keycloak_before";

describe("Realm settings test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const realmSettingsPage = new RealmSettingsPage();

  const managedAccessSwitch = "user-managed-access-switch";
  const userRegSwitch = "user-reg-switch";
  const forgotPwdSwitch = "forgot-pw-switch";
  const rememberMeSwitch = "remember-me-switch";
  const verifyEmailSwitch = "verify-email-switch";

  describe("Realm settings", function () {
    beforeEach(function () {
      keycloakBefore();
      loginPage.logIn();
    });

    it("Go to general tab", function () {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.toggleSwitch(managedAccessSwitch);
      realmSettingsPage.saveGeneral();
      realmSettingsPage.toggleSwitch(managedAccessSwitch);
      realmSettingsPage.saveGeneral();
    });

    it("Go to login tab", function () {
      sidebarPage.goToRealmSettings();
      cy.getId("rs-login-tab").click();
      realmSettingsPage.toggleSwitch(userRegSwitch);
      realmSettingsPage.toggleSwitch(forgotPwdSwitch);
      realmSettingsPage.toggleSwitch(rememberMeSwitch);
      realmSettingsPage.toggleSwitch(verifyEmailSwitch);
    });

    it("Go to themes tab", function () {
      sidebarPage.goToRealmSettings();
      cy.getId("rs-themes-tab").click();
      realmSettingsPage.selectLoginThemeType("keycloak");
      realmSettingsPage.selectAccountThemeType("keycloak");
      realmSettingsPage.selectAdminThemeType("keycloak.v2");
      realmSettingsPage.selectEmailThemeType("base");

      realmSettingsPage.saveThemes();
    });
  });
});
