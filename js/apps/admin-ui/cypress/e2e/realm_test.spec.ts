import { v4 as uuid } from "uuid";
import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import CreateRealmPage from "../support/pages/admin-ui/CreateRealmPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import RealmSettings from "../support/pages/admin-ui/configure/realm_settings/RealmSettings";
import ModalUtils from "../support/util/ModalUtils";
import CommonPage from "../support/pages/CommonPage";

const masthead = new Masthead();
const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const createRealmPage = new CreateRealmPage();
const realmSettings = new RealmSettings();
const modalUtils = new ModalUtils();
const commonPage = new CommonPage();

const testRealmName = "Test-realm-" + uuid();
const newRealmName = "New-Test-realm-" + uuid();
const editedRealmName = "Edited-Test-realm-" + uuid();
const testDisabledName = "Test-Disabled";
const specialCharsName = "%22-" + uuid();

describe("Realm tests", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
  });

  after(() =>
    Promise.all(
      [testRealmName, newRealmName, editedRealmName, specialCharsName].map(
        (realm) => adminClient.deleteRealm(realm),
      ),
    ),
  );

  it("should fail creating duplicated or empty name realm", () => {
    sidebarPage.goToCreateRealm();

    createRealmPage.createRealm().verifyRealmNameFieldInvalid();

    createRealmPage.fillRealmName("master").createRealm();

    masthead.checkNotificationMessage(
      "Could not create realm Conflict detected. See logs for details",
    );

    createRealmPage.cancelRealmCreation();
  });

  it("should create Test realm", () => {
    sidebarPage.goToCreateRealm();

    // Test and clear resource field
    createRealmPage.fillCodeEditor();
    createRealmPage.clearTextField();

    createRealmPage.fillRealmName(testRealmName).createRealm();

    masthead.checkNotificationMessage("Realm created successfully");
  });

  it("CRUD test of Disabled realm", () => {
    sidebarPage.goToCreateRealm();
    sidebarPage.waitForPageLoad();

    createRealmPage.fillRealmName(testDisabledName).createRealm();

    masthead.checkNotificationMessage("Realm created successfully");

    cy.reload();
    sidebarPage.goToRealm(testDisabledName).goToRealmSettings();

    createRealmPage.disableRealm();
    modalUtils.confirmModal();

    masthead.checkNotificationMessage("Realm successfully updated");

    sidebarPage.goToRealm(testDisabledName).goToRealmSettings();
    realmSettings.clickActionMenu();
    cy.findByText("Delete").click();
    modalUtils.cancelModal();

    cy.reload();
    sidebarPage.waitForPageLoad();
    sidebarPage.goToRealm(testDisabledName).goToRealmSettings();
    realmSettings.clickActionMenu();
    cy.findByText("Delete").click();
    modalUtils.confirmModal();
    masthead.checkNotificationMessage("The realm has been deleted");

    // Show current realms
    sidebarPage.realmExists("Test-Disabled", false);
  });

  it("should create realm from new a realm", () => {
    sidebarPage.goToCreateRealm();
    createRealmPage.fillRealmName(newRealmName).createRealm();

    masthead.checkNotificationMessage("Realm created successfully");

    sidebarPage.goToCreateRealm();
    createRealmPage.fillRealmName(editedRealmName).createRealm();

    masthead.checkNotificationMessage("Realm created successfully");

    // Show current realms
    sidebarPage.showCurrentRealms(4);
  });

  it("should change to Test realm", () => {
    sidebarPage.goToRealm(editedRealmName);
    sidebarPage.getCurrentRealm().should("eq", editedRealmName);

    sidebarPage
      .goToRealm(testRealmName)
      .getCurrentRealm()
      .should("eq", testRealmName);
  });

  it("should create realm with special characters", () => {
    sidebarPage.goToCreateRealm();
    createRealmPage.fillRealmName(specialCharsName).createRealm();

    sidebarPage.goToRealm(specialCharsName);
    sidebarPage.goToClients();
    commonPage.tableUtils().checkRowItemExists("account");
  });
});
