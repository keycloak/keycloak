import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateRealmPage from "../support/pages/admin_console/CreateRealmPage";
import Masthead from "../support/pages/admin_console/Masthead";
import adminClient from "../support/util/AdminClient";
import {
  keycloakBefore,
  keycloakBeforeEach,
} from "../support/util/keycloak_hooks";

const masthead = new Masthead();
const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const createRealmPage = new CreateRealmPage();

describe("Realms test", () => {
  const testRealmName =
    "Test realm " + (Math.random() + 1).toString(36).substring(7);
  const newRealmName =
    "New Test realm " + (Math.random() + 1).toString(36).substring(7);
  const editedRealmName =
    "Edited Test realm " + (Math.random() + 1).toString(36).substring(7);
  describe("Realm creation", () => {
    before(() => {
      keycloakBefore();
      loginPage.logIn();
    });

    beforeEach(() => {
      keycloakBeforeEach();
    });

    after(() => {
      [testRealmName, newRealmName, editedRealmName].map((realm) =>
        adminClient.deleteRealm(realm)
      );
    });

    it("should fail creating Master realm", () => {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName("master").createRealm();

      masthead.checkNotificationMessage(
        "Could not create realm Conflict detected. See logs for details"
      );
      createRealmPage.cancelRealmCreation();
      cy.reload();
    });

    it("should create Test realm", () => {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName(testRealmName).createRealm();

      masthead.checkNotificationMessage("Realm created");
    });

    it("should create realm from new a realm", () => {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName(newRealmName).createRealm();

      const fetchUrl = "/auth/admin/realms?briefRepresentation=true";
      cy.intercept(fetchUrl).as("fetch");

      masthead.checkNotificationMessage("Realm created");

      cy.wait(["@fetch"]);

      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName(editedRealmName).createRealm();

      masthead.checkNotificationMessage("Realm created");

      cy.wait(["@fetch"]);
    });

    it("should change to Test realm", () => {
      sidebarPage.getCurrentRealm().should("eq", editedRealmName);

      sidebarPage
        .goToRealm(testRealmName)
        .getCurrentRealm()
        .should("eq", testRealmName);
    });
  });
});
