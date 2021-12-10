import CreateRealmPage from "../support/pages/admin_console/CreateRealmPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import PartialImportModal from "../support/pages/admin_console/configure/realm_settings/PartialImportModal";
import RealmSettings from "../support/pages/admin_console/configure/realm_settings/RealmSettings";
import { keycloakBefore } from "../support/util/keycloak_before";
import AdminClient from "../support/util/AdminClient";

describe("Partial import test", () => {
  const TEST_REALM = "partial-import-test-realm";
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const createRealmPage = new CreateRealmPage();
  const modal = new PartialImportModal();
  const realmSettings = new RealmSettings();

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();

    // doing this from the UI has the added bonus of putting you in the test realm
    sidebarPage.goToCreateRealm();
    createRealmPage.fillRealmName(TEST_REALM).createRealm();

    sidebarPage.goToRealmSettings();
    realmSettings.clickActionMenu();
  });

  afterEach(async () => {
    const client = new AdminClient();
    await client.deleteRealm(TEST_REALM);
  });

  it("Opens and closes partial import dialog", () => {
    modal.open();
    modal.importButton().should("be.disabled");
    modal.cancelButton().click();
    modal.importButton().should("not.exist");
  });

  it("Import button only enabled if JSON has something to import", () => {
    modal.open();
    cy.get(".pf-c-code-editor__code textarea").type("{}");
    modal.importButton().should("be.disabled");
  });

  it("Displays user options after multi-realm import", () => {
    modal.open();
    modal.typeResourceFile("multi-realm.json");

    // Import button should be disabled if no checkboxes selected
    modal.importButton().should("be.disabled");
    modal.usersCheckbox().click();
    modal.importButton().should("be.enabled");
    modal.groupsCheckbox().click();
    modal.importButton().should("be.enabled");
    modal.groupsCheckbox().click();
    modal.usersCheckbox().click();
    modal.importButton().should("be.disabled");

    // verify resource counts
    modal.userCount().contains("1 Users");
    modal.groupCount().contains("1 Groups");
    modal.clientCount().contains("1 Clients");
    modal.idpCount().contains("1 Identity providers");
    modal.realmRolesCount().contains("2 Realm roles");
    modal.clientRolesCount().contains("1 Client roles");

    // import button should disable when switching realms
    modal.usersCheckbox().click();
    modal.importButton().should("be.enabled");
    modal.selectRealm("realm2");
    modal.importButton().should("be.disabled");

    modal.clientCount().contains("2 Clients");

    modal.clientsCheckbox().click();
    modal.importButton().click();

    cy.contains("2 records added");
    cy.contains("customer-portal");
    cy.contains("customer-portal2");
  });

  it("Displays user options after realmless import and does the import", () => {
    modal.open();

    modal.typeResourceFile("client-only.json");

    modal.realmSelector().should("not.exist");

    modal.clientCount().contains("1 Clients");

    modal.usersCheckbox().should("not.exist");
    modal.groupsCheckbox().should("not.exist");
    modal.idpCheckbox().should("not.exist");
    modal.realmRolesCheckbox().should("not.exist");
    modal.clientRolesCheckbox().should("not.exist");

    modal.clientsCheckbox().click();
    modal.importButton().click();

    cy.contains("One record added");
    cy.contains("customer-portal");
  });

  // Unfortunately, the PatternFly FileUpload component does not create an id for the clear button.  So we can't easily test that function right now.
});
