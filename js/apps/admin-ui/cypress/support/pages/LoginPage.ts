export default class LoginPage {
  #userNameInput = "#username";
  #passwordInput = "#password";
  #submitBtn = "#kc-login";

  #oldLoadContainer = "#loading";
  #loadContainer = "div.keycloak__loading-container";

  isLogInPage() {
    cy.get(this.#userNameInput).should("exist");
    cy.url().should("include", "/auth");

    return this;
  }

  logIn(userName = "admin", password = "admin") {
    cy.session(
      [userName, password],
      () => {
        cy.visit("/");

        cy.get('[role="progressbar"]').should("not.exist");
        cy.get(this.#oldLoadContainer).should("not.exist");
        cy.get(this.#loadContainer).should("not.exist");

        cy.get(this.#userNameInput).type(userName);
        cy.get(this.#passwordInput).type(password);

        cy.get(this.#submitBtn).click();
      },
      {
        validate() {
          cy.get('[role="progressbar"]').should("not.exist");
        },
      },
    );
  }
}
