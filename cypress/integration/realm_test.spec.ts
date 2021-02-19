import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateRealmPage from "../support/pages/admin_console/CreateRealmPage";
import Masthead from "../support/pages/admin_console/Masthead";
import AdminClient from "../support/util/AdminClient";

const masthead = new Masthead();
const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const createRealmPage = new CreateRealmPage();

describe("Realms test", function () {
  const testRealmName = "Test realm";
  describe("Realm creation", function () {
    beforeEach(function () {
      cy.visit("");
      loginPage.logIn();
    });

    after(async () => {
      const client = new AdminClient();
      await client.deleteRealm(testRealmName);
    });

    it("should fail creating Master realm", function () {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName("master").createRealm();

      masthead.checkNotificationMessage(
        "Could not create realm Conflict detected. See logs for details"
      );
    });

    it("should create Test realm", function () {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName(testRealmName).createRealm();

      masthead.checkNotificationMessage("Realm created");
    });

    it("should change to Test realm", function () {
      sidebarPage.getCurrentRealm().should("eq", "Master");

      sidebarPage
        .goToRealm(testRealmName)
        .getCurrentRealm()
        .should("eq", testRealmName);
    });
  });
});
