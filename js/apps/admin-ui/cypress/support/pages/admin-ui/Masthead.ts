import CommonElements from "../CommonElements";
export default class Masthead extends CommonElements {
  #logoBtn = ".pf-v5-c-page__header-brand-link img";
  #helpBtn = "#help";

  #userDrpDwn = "#user-dropdown";
  #userDrpDwnKebab = "#user-dropdown-kebab";
  #lastAlert = "last-alert";
  #globalAlerts = "global-alerts";
  #documentationLink = "#link";
  #backToAdminConsoleLink = "referrer-link";
  #userDrpdwnItem = ".pf-v5-c-menu__item";

  #getLastAlert() {
    return cy.findByTestId(this.#lastAlert);
  }

  #getAlerts() {
    return cy.findAllByTestId(this.#globalAlerts);
  }

  checkIsAdminUI() {
    cy.get(this.#logoBtn).should("exist");
    cy.get(this.#userDrpDwn).should("exist");

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
    cy.get(this.#helpBtn).click();
    cy.get("#enableHelp").click({ force: true });
  }

  toggleUsernameDropdown() {
    this.userDropdown().click();
    return this;
  }

  toggleMobileViewHelp() {
    cy.get(this.#userDrpdwnItem).contains("Help").click();
    return this;
  }

  clickRealmInfo() {
    cy.get(this.#userDrpdwnItem).contains("Realm info").click();
    return this;
  }

  clickGlobalHelp() {
    cy.get(this.#helpBtn).click();
    return this;
  }

  getDocumentationLink() {
    return cy.get(this.#documentationLink);
  }

  clickDocumentationLink() {
    this.getDocumentationLink()
      .find("a")
      .invoke("removeAttr", "target")
      .click();
    return this;
  }

  goToAdminConsole() {
    cy.findByTestId(this.#backToAdminConsoleLink).click({ force: true });
    return this;
  }

  userDropdown() {
    return cy
      .document()
      .then(({ documentElement }) => documentElement.getBoundingClientRect())
      .then(({ width }) =>
        cy.get(width < 1024 ? this.#userDrpDwnKebab : this.#userDrpDwn),
      );
  }

  signOut() {
    this.toggleUsernameDropdown();
    cy.get("#sign-out").click();
    Cypress.session.clearAllSavedSessions();
  }

  accountManagement() {
    this.toggleUsernameDropdown();
    cy.get("#manage-account").click();
  }

  checkNotificationMessage(message: string | RegExp, closeNotification = true) {
    const alertElement = this.#getLastAlert();

    if (typeof message === "string") {
      alertElement.should(($el) => expect($el).to.contain.text(message));
    } else {
      alertElement.should(($el) => expect($el).to.match(message));
    }

    if (closeNotification) {
      this.#getLastAlert().find("button").last().click({ force: true });
    }
    return this;
  }

  closeLastAlertMessage() {
    this.#getLastAlert().find("button").click();
    return this;
  }

  closeAllAlertMessages() {
    this.#getAlerts().find("button").click({
      force: true,
      multiple: true,
    });

    return this;
  }

  assertIsDesktopView() {
    cy.get(this.#userDrpDwn).should("be.visible");
    cy.get(this.#userDrpDwnKebab).should("not.be.visible");

    return this;
  }

  assertIsMobileView() {
    cy.get(this.#userDrpDwn).should("not.be.visible");
    cy.get(this.#userDrpDwnKebab).should("be.visible");

    return this;
  }
}
