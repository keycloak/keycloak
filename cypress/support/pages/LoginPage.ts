export default class LoginPage {
  private userNameInput = "#username";
  private passwordInput = "#password";
  private submitBtn = "#kc-login";
  private userDrpDwn = "#user-dropdown";

  private errorText = ".kc-feedback-text";
  private oldLoadContainer = "#loading";
  private loadContainer = "div.keycloak__loading-container";

  isLogInPage() {
    cy.get(this.userNameInput).should("exist");
    cy.url().should("include", "/auth");

    return this;
  }

  logIn(userName = "admin", password = "admin") {
    cy.get('[role="progressbar"]').should("not.exist");
    cy.get(this.oldLoadContainer).should("not.exist");
    cy.get(this.loadContainer).should("not.exist");

    cy.get("body")
      .children()
      .then((children) => {
        if (children.length == 1) {
          cy.get(this.userNameInput).type(userName);
          cy.get(this.passwordInput).type(password);

          cy.get(this.submitBtn).click();
        }
      });
    cy.get('[role="progressbar"]').should("not.exist");
  }

  checkErrorIsDisplayed() {
    cy.get(this.userDrpDwn).should("exist");

    return this;
  }

  checkErrorMessage(message: string) {
    cy.get(this.errorText).invoke("text").should("contain", message);

    return this;
  }
}
