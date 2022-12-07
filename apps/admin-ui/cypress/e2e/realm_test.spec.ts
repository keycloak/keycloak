import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import CreateRealmPage from "../support/pages/admin-ui/CreateRealmPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import RealmSettings from "../support/pages/admin-ui/configure/realm_settings/RealmSettings";
import ModalUtils from "../support/util/ModalUtils";

const masthead = new Masthead();
const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const createRealmPage = new CreateRealmPage();
const realmSettings = new RealmSettings();
const modalUtils = new ModalUtils();

const testRealmName =
  "Test realm " + (Math.random() + 1).toString(36).substring(7);
const newRealmName =
  "New Test realm " + (Math.random() + 1).toString(36).substring(7);
const editedRealmName =
  "Edited Test realm " + (Math.random() + 1).toString(36).substring(7);

describe("Realm tests", () => {
  before(() => {
    keycloakBefore();
    loginPage.logIn();
  });

  after(() =>
    Promise.all(
      [testRealmName, newRealmName, editedRealmName].map((realm) =>
        adminClient.deleteRealm(realm)
      )
    )
  );

  it("should fail creating Master realm", () => {
    sidebarPage.goToCreateRealm();
    createRealmPage.fillRealmName("master").createRealm();

    masthead.checkNotificationMessage(
      "Could not create realm Conflict detected. See logs for details"
    );
    createRealmPage.cancelRealmCreation();
  });

  it("should fail creating realm with empty name", () => {
    sidebarPage.goToCreateRealm();
    createRealmPage.createRealm();

    createRealmPage.verifyRealmNameFieldInvalid();
  });

  it("should create Test realm", () => {
    sidebarPage.goToCreateRealm();

    // Test and clear resource field
    createRealmPage.fillCodeEditor();
    createRealmPage.clearTextField();

    createRealmPage.fillRealmName(testRealmName).createRealm();

    masthead.checkNotificationMessage("Realm created successfully");
  });

  it("should create Test Disabled realm", () => {
    sidebarPage.goToCreateRealm();
    sidebarPage.waitForPageLoad();
    createRealmPage.fillRealmName("Test Disabled").createRealm();
    createRealmPage.disableRealm();

    masthead.checkNotificationMessage("Realm created successfully");
  });

  it("Should cancel deleting Test Disabled realm", () => {
    sidebarPage.goToRealm("Test Disabled").goToRealmSettings();
    realmSettings.clickActionMenu();
    cy.findByText("Delete").click();
    modalUtils.cancelModal();
  });

  it("Should delete Test Disabled realm", () => {
    sidebarPage.goToRealm("Test Disabled").goToRealmSettings();
    realmSettings.clickActionMenu();
    cy.findByText("Delete").click();
    modalUtils.confirmModal();
    masthead.checkNotificationMessage("The realm has been deleted");

    // Show current realms
    sidebarPage.showCurrentRealms(2);
  });

  it("should create realm from new a realm", () => {
    sidebarPage.goToCreateRealm();
    createRealmPage.fillRealmName(newRealmName).createRealm();

    const fetchUrl = "/admin/realms?briefRepresentation=true";
    cy.intercept(fetchUrl).as("fetch");

    masthead.checkNotificationMessage("Realm created successfully");

    cy.wait(["@fetch"]);

    sidebarPage.goToCreateRealm();
    createRealmPage.fillRealmName(editedRealmName).createRealm();

    masthead.checkNotificationMessage("Realm created successfully");

    cy.wait(["@fetch"]);

    // Show current realms
    sidebarPage.showCurrentRealms(4);
  });

  it("should change to Test realm", () => {
    sidebarPage.getCurrentRealm().should("eq", editedRealmName);

    sidebarPage
      .goToRealm(testRealmName)
      .getCurrentRealm()
      .should("eq", testRealmName);
  });
});
