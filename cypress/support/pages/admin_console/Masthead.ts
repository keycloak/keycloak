export default class Masthead {
  private menuBtn = "#nav-toggle";
  private logoBtn = "#masthead-logo";
  private helpBtn = "#help";
  private closeAlertMessageBtn = ".pf-c-alert__action button";
  private closeLastAlertMessageBtn =
    ".pf-c-alert-group > li:first-child .pf-c-alert__action button";

  private alertMessage = ".pf-c-alert__title";
  private userDrpDwn = "#user-dropdown";
  private userDrpDwnKebab = "#user-dropdown-kebab";

  checkIsAdminConsole() {
    cy.get(this.logoBtn).should("exist");
    cy.get(this.userDrpDwn).should("exist");

    return this;
  }

  get isMobileMode() {
    return window.parent[0].innerWidth < 1024;
  }

  setMobileMode(isMobileMode: boolean) {
    if (isMobileMode) {
      cy.viewport("iphone-6");
    } else {
      cy.viewport(1024, 768);
    }
  }

  toggleGlobalHelp() {
    cy.get(this.helpBtn).click();
    cy.get("#enableHelp").click({ force: true });
  }

  userDropdown() {
    if (this.isMobileMode) {
      return cy.get(this.userDrpDwnKebab);
    } else {
      return cy.get(this.userDrpDwn);
    }
  }

  signOut() {
    this.userDropdown().click();
    cy.get("#sign-out").click();
  }

  accountManagement() {
    this.userDropdown().click();
    cy.get("#manage-account").click();
  }

  checkNotificationMessage(message: string, closeNotification?: boolean) {
    cy.get(this.alertMessage)
      .should("contain.text", message)
      .parent()
      .within(() => {
        if (closeNotification) {
          cy.get(".pf-c-alert__action").click();
        }
      });
    return this;
  }

  closeLastAlertMessage() {
    cy.get(this.closeLastAlertMessageBtn).click();
    return this;
  }

  closeAllAlertMessages() {
    cy.get(this.closeAlertMessageBtn).click({ multiple: true });
    return this;
  }

  checkKebabShown() {
    cy.get(this.userDrpDwn).should("not.be.visible");
    cy.get(this.userDrpDwnKebab).should("exist");

    return this;
  }
}
