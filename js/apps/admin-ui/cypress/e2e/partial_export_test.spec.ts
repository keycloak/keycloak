import PartialExportModal from "../support/pages/admin-ui/configure/realm_settings/PartialExportModal";
import RealmSettings from "../support/pages/admin-ui/configure/realm_settings/RealmSettings";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";

describe("Partial realm export", () => {
  const REALM_NAME = "Partial-export-test-realm";

  before(() => adminClient.createRealm(REALM_NAME));

  after(() => adminClient.deleteRealm(REALM_NAME));

  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const modal = new PartialExportModal();
  const realmSettings = new RealmSettings();

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealm(REALM_NAME).goToRealmSettings();
    realmSettings.clickActionMenu();
    modal.open();
  });

  it("Closes the dialog", () => {
    modal.cancelButton().click();
    modal.exportButton().should("not.exist");
  });

  it("Shows a warning message", () => {
    modal.warningMessage().should("not.exist");

    modal.includeGroupsAndRolesSwitch().click({ force: true });
    modal.warningMessage().should("exist");
    modal.includeGroupsAndRolesSwitch().click({ force: true });
    modal.warningMessage().should("not.exist");

    modal.includeClientsSwitch().click({ force: true });
    modal.warningMessage().should("exist");
    modal.includeClientsSwitch().click({ force: true });
    modal.warningMessage().should("not.exist");
  });

  it("Exports the realm", () => {
    modal.includeGroupsAndRolesSwitch().click({ force: true });
    modal.includeClientsSwitch().click({ force: true });
    modal.exportButton().click();
    cy.readFile(
      Cypress.config("downloadsFolder") + "/realm-export.json",
    ).should("exist");
    modal.exportButton().should("not.exist");
  });
});
