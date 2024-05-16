export default class GroupModal {
  #openPartialImport = "openPartialImportModal";

  open() {
    cy.findByTestId(this.#openPartialImport).click();
    return this;
  }

  typeResourceFile = (filename: string) => {
    cy.get("#partial-import-file-filename").selectFile(
      "cypress/fixtures/partial-import-test-data/" + filename,
      { action: "drag-drop" },
    );
  };

  textArea() {
    return cy.get(".pf-v5-c-code-editor__code textarea");
  }

  importButton() {
    return cy.findByTestId("import-button");
  }

  cancelButton() {
    return cy.findByTestId("cancel-button");
  }

  clearButton() {
    return cy.get("button").contains("Clear");
  }

  clickClearConfirmButton() {
    cy.findByTestId("clear-button").click();
  }

  closeButton() {
    return cy.findByTestId("close-button");
  }

  usersCheckbox() {
    return cy.findByTestId("users-checkbox");
  }

  clientsCheckbox() {
    return cy.findByTestId("clients-checkbox");
  }

  groupsCheckbox() {
    return cy.findByTestId("groups-checkbox");
  }

  idpCheckbox() {
    return cy.findByTestId("identityProviders-checkbox");
  }

  realmRolesCheckbox() {
    return cy.findByTestId("realmRoles-checkbox");
  }

  clientRolesCheckbox() {
    return cy.findByTestId("clientRoles-checkbox");
  }

  userCount() {
    return this.usersCheckbox().get("label");
  }

  clientCount() {
    return this.clientsCheckbox().get("label");
  }

  groupCount() {
    return this.groupsCheckbox().get("label");
  }

  idpCount() {
    return this.idpCheckbox().get("label");
  }

  realmRolesCount() {
    return this.realmRolesCheckbox().get("label");
  }

  clientRolesCount() {
    return this.clientRolesCheckbox().get("label");
  }

  realmSelector() {
    return cy.get("#realm-selector");
  }

  selectRealm(realm: string) {
    this.realmSelector().click();
    cy.findByTestId(realm + "-select-option").click();
  }
}
