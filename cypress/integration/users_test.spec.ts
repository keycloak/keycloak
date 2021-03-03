import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";

describe("Users test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();

  describe("User creation", () => {
    beforeEach(function () {
      cy.visit("");
      loginPage.logIn();
      sidebarPage.goToUsers();
    });

    it("Go to create User page", () => {
      cy.get("[data-testid=add-user").click();
      cy.url().should("include", "users/add-user");
      cy.get("[data-testid=cancel-create-user").click();
      cy.url().should("not.include", "/add-user");
    });
  });
});
