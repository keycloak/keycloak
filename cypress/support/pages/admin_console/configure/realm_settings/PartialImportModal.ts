export default class GroupModal {
  private openPartialImport = "openPartialImportModal";

  open() {
    cy.findByTestId(this.openPartialImport).click();
    return this;
  }

  typeResourceFile = (filename: string) => {
    cy.readFile("cypress/fixtures/partial-import-test-data/" + filename).then(
      (myJSON) => {
        const text = JSON.stringify(myJSON);

        cy.get("#partial-import-file").type(text, {
          parseSpecialCharSequences: false,
          delay: 0,
        });
      }
    );
  };

  importButton() {
    return cy.findByTestId("import-button");
  }

  cancelButton() {
    return cy.findByTestId("cancel-button");
  }

  groupsCheckbox() {
    return cy.findByTestId("groups-checkbox");
  }

  usersCheckbox() {
    return cy.findByTestId("users-checkbox");
  }

  userCount() {
    return cy.findByTestId("users-count");
  }

  clientCount() {
    return cy.findByTestId("clients-count");
  }

  groupCount() {
    return cy.findByTestId("groups-count");
  }

  idpCount() {
    return cy.findByTestId("identityProviders-count");
  }

  realmRolesCount() {
    return cy.findByTestId("realmRoles-count");
  }

  clientRolesCount() {
    return cy.findByTestId("clientRoles-count");
  }

  realmSelector() {
    return cy.get("#realm-selector");
  }

  selectRealm(realm: string) {
    this.realmSelector().click();
    cy.findByTestId(realm + "-select-option").click();
  }
}
