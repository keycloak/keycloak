export default class PartialExportModal {
  open() {
    cy.findByTestId("openPartialExportModal").click();
  }

  exportButton() {
    return cy.findByTestId("export-button");
  }

  cancelButton() {
    return cy.findByTestId("cancel-button");
  }

  includeGroupsAndRolesSwitch() {
    return cy.get("#include-groups-and-roles-check");
  }

  includeClientsSwitch() {
    return cy.get("#include-clients-check");
  }

  warningMessage() {
    return cy.findByTestId("warning-message");
  }
}
