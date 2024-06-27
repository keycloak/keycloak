import CommonElements from "../CommonElements";
export default class Masthead extends CommonElements {
  #logoBtn = ".pf-v5-c-page__header-brand-link img";
  #helpBtn = "#help";
  #closeAlertMessageBtn = ".pf-v5-c-alert__action button";
  #closeLastAlertMessageBtn = "li:first-child .pf-v5-c-alert__action button";

  #alertMessage = ".pf-v5-c-alert__title";
  #userDrpDwn = "#user-dropdown";
  #userDrpDwnKebab = "#user-dropdown-kebab";
  #globalAlerts = "global-alerts";
  #documentationLink = "#link";
  #backToAdminConsoleLink = "referrer-link";
  #userDrpdwnItem = ".pf-v5-c-menu__item";

  #getAlertsContainer() {
    return cy.findByTestId(this.#globalAlerts);
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
    if (typeof message === "string") {
      this.#getAlertsContainer()
        .find(this.#alertMessage)
        .should("contain.text", message);

      if (closeNotification) {
        this.#getAlertsContainer()
          .find(`button[title="` + message.replaceAll('"', '\\"') + `"]`)
          .last()
          .click({ force: true });
      }
    } else {
      this.#getAlertsContainer()
        .find(this.#alertMessage)
        .invoke("text")
        .should("match", message);

      if (closeNotification) {
        this.#getAlertsContainer().find("button").last().click({ force: true });
      }
    }
    return this;
  }

  closeLastAlertMessage() {
    this.#getAlertsContainer().find(this.#closeLastAlertMessageBtn).click();
    return this;
  }

  closeAllAlertMessages() {
    this.#getAlertsContainer().find(this.#closeAlertMessageBtn).click({
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
