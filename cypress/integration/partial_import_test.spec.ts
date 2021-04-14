import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import PartialImportModal from "../support/pages/admin_console/configure/realm_settings/PartialImportModal";
import RealmSettings from "../support/pages/admin_console/configure/realm_settings/RealmSettings";
import { keycloakBefore } from "../support/util/keycloak_before";

describe("Partial import test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const partialImportModal = new PartialImportModal();
  const realmSettings = new RealmSettings();

  beforeEach(function () {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToRealmSettings();
    realmSettings.clickActionMenu();
  });

  it("Opens and closes partial import dialog", () => {
    partialImportModal.open();
    cy.getId("import-button").should("be.disabled");
    cy.getId("cancel-button").click();
    cy.getId("import-button").should("not.exist");
  });

  it("Import button reacts to loaded json", () => {
    partialImportModal.open();
    cy.get("#partial-import-file").type("{}");
    cy.getId("import-button").should("be.enabled");
  });

  // Unfortunately, the PatternFly FileUpload component does not create an id for the clear button.  So we can't easily test that function right now.
});
