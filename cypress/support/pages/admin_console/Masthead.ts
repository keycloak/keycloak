export default class Masthead {
  private menuBtn = "#nav-toggle";
  private logoBtn = "#masthead-logo";
  private helpBtn = "#help";

  private userDrpDwn = "#user-dropdown";
  private userDrpDwnKebab = "#user-dropdown-kebab";
  private isMobile = false;

  isAdminConsole() {
    cy.get(this.logoBtn).should("exist");
    cy.get(this.userDrpDwn).should("exist");

    return this;
  }

  get isMobileMode() {
    return this.isMobile;
  }

  setMobileMode(isMobileMode: boolean) {
    this.isMobile = isMobileMode;
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

  checkNotificationMessage(message: string) {
    cy.contains(message).should("exist");

    return this;
  }

  checkKebabShown() {
    cy.get(this.userDrpDwn).should("not.exist");
    cy.get(this.userDrpDwnKebab).should("exist");

    return this;
  }
}
