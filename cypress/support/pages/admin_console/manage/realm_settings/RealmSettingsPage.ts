export default class RealmSettingsPage {
    saveBtn: string;
    loginTab: string;
    managedAccessSwitch: string;
    userRegSwitch: string;
    forgotPwdSwitch: string;
    rememberMeSwitch: string;
    emailAsUsernameSwitch: string;
    loginWithEmailSwitch: string;
    duplicateEmailsSwitch: string;
    verifyEmailSwitch: string;

  
    constructor() {
      this.saveBtn = "general-tab-save";
      this.loginTab = "rs-login-tab";
      this.managedAccessSwitch = "user-managed-access-switch";
      this.userRegSwitch = "user-reg-switch"
      this.forgotPwdSwitch = "forgot-password-switch"
      this.rememberMeSwitch = "remember-me-switch"
      this.emailAsUsernameSwitch = "email-as-username-switch"
      this.loginWithEmailSwitch = "login-with-email-switch"
      this.duplicateEmailsSwitch = "duplicate-emails-switch"
      this.verifyEmailSwitch = "verify-email-switch"

    }
  
    toggleSwitch(switchName: string) {
        cy.getId(switchName).next().click();
  
      return this;
    }
  
    save() {
      cy.getId(this.saveBtn).click();
  
      return this;
    }
  }
  