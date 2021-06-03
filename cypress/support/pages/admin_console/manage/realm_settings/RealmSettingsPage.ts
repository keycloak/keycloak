export default class RealmSettingsPage {
  generalSaveBtn = "general-tab-save";
  themesSaveBtn = "themes-tab-save";
  loginTab = "rs-login-tab";
  selectLoginTheme = "#kc-login-theme";
  loginThemeList = "#kc-login-theme + ul";
  selectAccountTheme = "#kc-account-theme";
  accountThemeList = "#kc-account-theme + ul";
  selectAdminTheme = "#kc-admin-console-theme";
  adminThemeList = "#kc-admin-console-theme + ul";
  selectEmailTheme = "#kc-email-theme";
  emailThemeList = "#kc-email-theme + ul";
  selectDefaultLocale = "select-default-locale";
  defaultLocaleList = "select-default-locale + ul";
  emailSaveBtn = "email-tab-save";
  managedAccessSwitch = "user-managed-access-switch";
  userRegSwitch = "user-reg-switch";
  forgotPwdSwitch = "forgot-pw-switch";
  rememberMeSwitch = "remember-me-switch";
  emailAsUsernameSwitch = "email-as-username-switch";
  loginWithEmailSwitch = "login-with-email-switch";
  duplicateEmailsSwitch = "duplicate-emails-switch";
  verifyEmailSwitch = "verify-email-switch";
  authSwitch = "email-authentication-switch";
  fromInput = "sender-email-address";
  enableSslCheck = "enable-ssl";
  enableStartTlsCheck = "enable-start-tls";
  addProviderDropdown = "addProviderDropdown";
  addProviderButton = "add-provider-button";
  displayName = "display-name-input";

  selectLoginThemeType(themeType: string) {
    const themesUrl = "/auth/admin/realms/master/themes";
    cy.intercept(themesUrl).as("themesFetch");

    cy.get(this.selectLoginTheme).click();
    cy.get(this.loginThemeList).contains(themeType).click();

    return this;
  }

  selectAccountThemeType(themeType: string) {
    cy.get(this.selectAccountTheme).click();
    cy.get(this.accountThemeList).contains(themeType).click();
    return this;
  }

  selectAdminThemeType(themeType: string) {
    cy.get(this.selectAdminTheme).click();
    cy.get(this.adminThemeList).contains(themeType).click();
    return this;
  }

  selectEmailThemeType(themeType: string) {
    cy.get(this.selectEmailTheme).click();
    cy.get(this.emailThemeList).contains(themeType).click();
    return this;
  }

  setDefaultLocale(locale: string) {
    cy.get(this.selectDefaultLocale).click();
    cy.get(this.defaultLocaleList).contains(locale).click();
    return this;
  }

  saveGeneral() {
    cy.getId(this.generalSaveBtn).click();

    return this;
  }

  saveThemes() {
    cy.getId(this.themesSaveBtn).click();

    return this;
  }

  addSenderEmail(senderEmail: string) {
    cy.getId(this.fromInput).clear();

    if (senderEmail) {
      cy.getId(this.fromInput).type(senderEmail);
    }

    return this;
  }

  toggleSwitch(switchName: string) {
    cy.getId(switchName).next().click();

    return this;
  }

  toggleCheck(switchName: string) {
    cy.getId(switchName).click();

    return this;
  }

  toggleAddProviderDropdown() {
    const keysUrl = "/auth/admin/realms/master/keys";
    cy.intercept(keysUrl).as("keysFetch");
    cy.getId(this.addProviderDropdown).click();

    return this;
  }

  addProvider() {
    cy.getId(this.addProviderButton).click();

    return this;
  }

  enterConsoleDisplayName(name: string) {
    cy.getId(this.displayName).clear().type(name);
  }

  save(saveBtn: string) {
    cy.getId(saveBtn).click();

    return this;
  }
}
