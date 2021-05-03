import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateRealmPage from "../support/pages/admin_console/CreateRealmPage";
import Masthead from "../support/pages/admin_console/Masthead";
import AdminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_before";
import RealmSelector from "../support/pages/admin_console/RealmSelector";

const masthead = new Masthead();
const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const createRealmPage = new CreateRealmPage();
const realmSelector = new RealmSelector();

describe("Realms test", () => {
  const testRealmName = "Test realm";
  describe("Realm creation", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
    });

    after(async () => {
      const client = new AdminClient();
      await client.deleteRealm(testRealmName);
    });

    it("should fail creating Master realm", () => {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName("master").createRealm();

      masthead.checkNotificationMessage(
        "Could not create realm Conflict detected. See logs for details"
      );
    });

    it("should create Test realm", () => {
      sidebarPage.goToCreateRealm();
      createRealmPage.fillRealmName(testRealmName).createRealm();

      masthead.checkNotificationMessage("Realm created");
    });

    it("should change to Test realm", () => {
      sidebarPage.getCurrentRealm().should("eq", "Master");

      sidebarPage
        .goToRealm(testRealmName)
        .getCurrentRealm()
        .should("eq", testRealmName);
    });
  });

  describe("More then 5 realms", () => {
    const realmNames = ["One", "Two", "Three", "Four", "Five"];

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      for (const realmName of realmNames) {
        sidebarPage.goToCreateRealm();
        createRealmPage.fillRealmName(realmName).createRealm();
        sidebarPage.goToClients();
      }
    });

    afterEach(async () => {
      const client = new AdminClient();
      for (const realmName of realmNames) {
        await client.deleteRealm(realmName);
      }
    });

    it("switch to searchable realm selector", () => {
      realmSelector.openRealmContextSelector().shouldContainAll(realmNames);
    });
  });
});
