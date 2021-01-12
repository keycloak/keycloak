export default class WelcomePage {

    constructor() {
        this.userNameInput = "#username";
        this.passwordInput = "#password";
        this.confirmPasswordInput = "#passwordConfirmation";
        this.createBtn = "#create-button";

        this.adminConsoleBtn = ".welcome-primary-link a";
    }

    createAdminUser(userName = "admin", password = "admin") {
        cy.get(this.userNameInput).type(userName);
        cy.get(this.passwordInput).type(password);
        cy.get(this.confirmPasswordInput).type(password);

        cy.get(this.createBtn).click();

        return this;
    }

    goToAdminConsole() {
        cy.get(this.adminConsoleBtn).click();

        return this;
    }
}