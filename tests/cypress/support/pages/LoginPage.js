export default class LoginPage {

  constructor() {
    this.userNameInput = "#username";
    this.passwordInput = "#password";
    this.submitBtn = "#kc-login";

    this.errorText = ".kc-feedback-text";
    this.oldLoadContainer = "#loading";
    this.loadContainer = "div.keycloak__loading-container";
  }

  isLogInPage() {
    cy.get(this.userNameInput).should("exist");
    cy.url().should("include", "/auth");

    return this;
  }

  logIn(userName = "admin", password = "admin") {
    cy.get(this.oldLoadContainer).should("not.exist");
    cy.get(this.loadContainer).should("not.exist");

    cy.get("body").children().then((children) => {
      if(children.length == 1) {
        cy.get(this.userNameInput).type(userName);
        cy.get(this.passwordInput).type(password);
    
        cy.get(this.submitBtn).click();
      }
    });
  }

  checkErrorIsDisplayed() {
    cy.get(this.userDrpDwn).should("exist");

    return this;
  }

  checkErrorMessage(message) {
    cy.get(this.errorText).invoke("text").should("contain", message);

    return this;
  }
}