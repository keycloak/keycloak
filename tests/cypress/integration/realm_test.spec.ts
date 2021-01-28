import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateRealmPage from "../support/pages/admin_console/CreateRealmPage";
import Masthead from "../support/pages/admin_console/Masthead";

const masthead = new Masthead();
const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const createRealmPage = new CreateRealmPage();

describe("Realms test", function () {

  describe("Realm creation", function () {
    beforeEach(function () {
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
