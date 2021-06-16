export default class GroupModal {
  private openPartialImport = "openPartialImportModal";

  open() {
    cy.getId(this.openPartialImport).click();
    return this;
  }

  typeResourceFile = (filename: string) => {
    cy.readFile(
      "cypress/integration/partial-import-test-data/" + filename
    ).then((myJSON) => {
      const text = JSON.stringify(myJSON);

      cy.get("#partial-import-file").type(text, {
        parseSpecialCharSequences: false,
      });
    });
  };

  importButton() {
    return cy.getId("import-button");
  }

  cancelButton() {
    return cy.getId("cancel-button");
  }

  groupsCheckbox() {
    return cy.getId("groups-checkbox");
  }

  usersCheckbox() {
    return cy.getId("users-checkbox");
  }

  userCount() {
    return cy.getId("users-count");
  }

  clientCount() {
    return cy.getId("clients-count");
  }

  groupCount() {
    return cy.getId("groups-count");
  }

  idpCount() {
    return cy.getId("identityProviders-count");
  }

  realmRolesCount() {
    return cy.getId("realmRoles-count");
  }

  clientRolesCount() {
    return cy.getId("clientRoles-count");
  }

  realmSelector() {
    return cy.get("#realm-selector");
  }

  selectRealm(realm: string) {
    this.realmSelector().click();
    cy.getId(realm + "-select-option").click();
  }
}
