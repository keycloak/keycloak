import CommonElements from "../CommonElements";
export default class Masthead extends CommonElements {
  private logoBtn = ".pf-c-page__header-brand-link img";
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
    return cy
      .document()
      .then(({ documentElement }) => documentElement.getBoundingClientRect())
      .then(({ width }) =>
        cy.get(width < 1024 ? this.userDrpDwnKebab : this.userDrpDwn)
      );
  }

  signOut() {
    this.userDropdown().click();
    cy.get("#sign-out").click();
  }

  accountManagement() {
    this.userDropdown().click();
    cy.get("#manage-account").click();
  }

  checkNotificationMessage(message: string, closeNotification = true) {
    cy.get(this.alertMessage).should("contain.text", message);

    if (closeNotification) {
      cy.get(`button[title="` + message.replaceAll('"', '\\"') + `"]`)
        .last()
        .click({ force: true });
    }
    return this;
  }

  closeLastAlertMessage() {
    cy.get(this.closeLastAlertMessageBtn).click();
    return this;
  }

  closeAllAlertMessages() {
    cy.get(this.closeAlertMessageBtn).each(() => {
      cy.get(this.closeAlertMessageBtn).click({ force: true, multiple: true });
    });
    return this;
  }

  checkKebabShown() {
    cy.get(this.userDrpDwn).should("not.be.visible");
    cy.get(this.userDrpDwnKebab).should("exist");

    return this;
  }
}
