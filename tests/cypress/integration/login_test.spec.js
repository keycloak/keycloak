import LoginPage from "./../support/pages/LoginPage.js";
import Masthead from "./../support/pages/admin_console/Masthead.js";

describe("Logging In", function () {
  const username = "admin";
  const password = "admin";

  const loginPage = new LoginPage();
  const masthead = new Masthead();

  describe("Login form submission", function () {
    beforeEach(function () {
      cy.visit("");
    });

    it("displays errors on login", function () {
      loginPage
        .logIn("wrong", "user{enter}")
        .checkErrorMessage("Invalid username or password.")
        .isLogInPage();
    });

    it("redirects to admin console on success", function () {
      loginPage.logIn(username, password);

      masthead.isAdminConsole();

      cy.getCookie("KEYCLOAK_SESSION_LEGACY").should("exist");
    });
  });
});
