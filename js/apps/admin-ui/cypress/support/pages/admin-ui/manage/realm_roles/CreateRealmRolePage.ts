class CreateRealmRolePage {
  #realmRoleNameInput = "name";
  #realmRoleNameError = "#name-helper";
  #realmRoleDescriptionInput = "description";
  #saveBtn = "save";
  #cancelBtn = "cancel";

  //#region General Settings
  fillRealmRoleData(name: string, description = "") {
    cy.findByTestId(this.#realmRoleNameInput).clear();

    if (name) {
      cy.findByTestId(this.#realmRoleNameInput).type(name);
    }

    if (description !== "") {
      this.updateDescription(description);
    }
    return this;
  }

  checkRealmRoleNameRequiredMessage() {
    cy.findByTestId(this.#realmRoleNameInput)
      .parent()
      .should("have.class", "pf-v5-c-form-control pf-m-error");

    return this;
  }
  //#endregion

  clickActionMenu(item: string) {
    cy.findByTestId("action-dropdown")
      .click()
      .parent()
      .within(() => {
        cy.findByText(item).click();
      });
    return this;
  }

  checkNameDisabled() {
    cy.findByTestId(this.#realmRoleNameInput).should(
      "have.attr",
      "disabled",
      "disabled",
    );
    return this;
  }

  checkDescription(description: string) {
    cy.findByTestId(this.#realmRoleDescriptionInput).should(
      "have.value",
      description,
    );
    return this;
  }

  updateDescription(description: string) {
    cy.findByTestId(this.#realmRoleDescriptionInput).clear();
    cy.findByTestId(this.#realmRoleDescriptionInput).type(description);
    return this;
  }

  save() {
    cy.findByTestId(this.#saveBtn).click();

    return this;
  }

  cancel() {
    cy.findByTestId(this.#cancelBtn).click();

    return this;
  }

  goToAttributesTab() {
    cy.findByTestId("attributesTab").click();
    return this;
  }
}

export default new CreateRealmRolePage();
