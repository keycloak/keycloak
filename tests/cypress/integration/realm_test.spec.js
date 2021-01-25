import LoginPage from "../support/pages/LoginPage.js";
import SidebarPage from "../support/pages/admin_console/SidebarPage.js";
import CreateRealmPage from "../support/pages/admin_console/CreateRealmPage.js";
import Masthead from "../support/pages/admin_console/Masthead.js";

describe("Realms test", function () {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const createRealmPage = new CreateRealmPage();
  const masthead = new Masthead();

  describe("Realm creation", function () {
    beforeEach(function () {
      cy.clearCookies();
      cy.visit("");
      loginPage.logIn();
    });

    it("should fail creating Master realm", function () {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName("master").createRealm();

      masthead.checkNotificationMessage(
        "Error: Request failed with status code 409"
      );
    });

    it("should create Test realm", function () {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName("Test").createRealm();

      masthead.checkNotificationMessage("Realm created");
    });

    it("should change to Test realm", function () {
      sidebarPage.getCurrentRealm().should("eq", "Master");

      sidebarPage.goToRealm("Test").getCurrentRealm().should("eq", "Test");
    });
  });
});
