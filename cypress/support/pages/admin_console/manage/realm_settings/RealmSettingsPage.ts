export default class RealmSettingsPage {
  saveBtnGeneral: string;
  saveBtnThemes: string;
  loginTab: string;
  selectLoginTheme: string;
  loginThemeList: string;
  selectAccountTheme: string;
  accountThemeList: string;
  selectAdminTheme: string;
  adminThemeList: string;
  selectEmailTheme: string;
  emailThemeList: string;
  selectDefaultLocale: string;
  defaultLocaleList: string;

  constructor() {
    this.saveBtnGeneral = "general-tab-save";
    this.saveBtnThemes = "themes-tab-save";
    this.loginTab = "rs-login-tab";
    this.selectLoginTheme = "#kc-login-theme";
    this.loginThemeList = "#kc-login-theme + ul";
    this.selectAccountTheme = "#kc-account-theme";
    this.accountThemeList = "#kc-account-theme + ul";
    this.selectAdminTheme = "#kc-admin-console-theme";
    this.adminThemeList = "#kc-admin-console-theme + ul";
    this.selectEmailTheme = "#kc-email-theme";
    this.emailThemeList = "#kc-email-theme + ul";
    this.selectDefaultLocale = "select-default-locale";
    this.defaultLocaleList = "select-default-locale + ul";
  }

  selectLoginThemeType(themeType: string) {
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

  toggleSwitch(switchName: string) {
    cy.getId(switchName).next().click();

    return this;
  }

  saveGeneral() {
    cy.getId(this.saveBtnGeneral).click();

    return this;
  }

  saveThemes() {
    cy.getId(this.saveBtnThemes).click();

    return this;
  }
}
