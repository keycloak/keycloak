import Select from "../../../../forms/Select";

export default class UserProfile {
  #userProfileTab = "rs-user-profile-tab";
  #attributesTab = "attributesTab";
  #attributesGroupTab = "attributesGroupTab";
  #jsonEditorTab = "jsonEditorTab";
  #createAttributeButton = "createAttributeBtn";
  #actionsDrpDwn = "actions-dropdown";
  #deleteDrpDwnOption = "deleteDropdownAttributeItem";
  #editDrpDwnOption = "editDropdownAttributeItem";
  #cancelNewAttribute = "attribute-cancel";
  #newAttributeNameInput = "attribute-name";
  #newAttributeDisplayNameInput = "attribute-display-name";
  #newAttributeEnabledWhen = 'input[name="enabledWhen"]';
  #newAttributeRequiredWhen = 'input[name="requiredWhen"]';
  #newAttributeEmptyValidators = ".kc-emptyValidators";
  #newAttributeAnnotationBtn = "annotations-add-row";
  #newAttributeAnnotationKey = "annotations.0.key";
  #newAttributeAnnotationValue = "annotations.0.value";
  #validatorRolesList = "#validator";
  #validatorsList = 'tbody [data-label="name"]';
  #saveNewAttributeBtn = "attribute-create";
  #addValidatorBtn = "addValidator";
  #saveValidatorBtn = "save-validator-role-button";
  #removeValidatorBtn = "deleteValidator";
  #deleteValidatorBtn = "confirm";
  #cancelAddingValidatorBtn = "cancel-validator-role-button";
  #cancelRemovingValidatorBtn = "cancel";
  #newAttributeRequiredField = "input#kc-required.pf-c-switch__input";
  #newAttributeUserEdit = "user-edit";
  #newAttributeAdminEdit = "admin-edit";
  #newAttributeUserView = "user-view";
  #newAttributeAdminView = "admin-view";
  #newAttributesGroupNameInput = "input#kc-name";
  #newAttributesGroupDisplayNameInput = 'input[name="displayHeader"]';
  #saveNewAttributesGroupBtn = "saveGroupBtn";

  goToTab() {
    cy.findByTestId(this.#userProfileTab).click();
    return this;
  }

  goToAttributesTab() {
    cy.findByTestId(this.#attributesTab).click();
    return this;
  }

  goToAttributesGroupTab() {
    cy.findByTestId(this.#attributesGroupTab).click();
    return this;
  }

  goToJsonEditorTab() {
    cy.findByTestId(this.#jsonEditorTab).click();
    return this;
  }

  createAttributeButtonClick() {
    cy.findByTestId(this.#createAttributeButton).click();
    return this;
  }

  selectDropdown() {
    cy.findByTestId(this.#actionsDrpDwn).click();
    return this;
  }

  selectDeleteOption() {
    cy.findByTestId(this.#deleteDrpDwnOption).click();
    return this;
  }

  selectEditOption() {
    cy.findByTestId(this.#editDrpDwnOption).click();
    return this;
  }

  cancelAttributeCreation() {
    cy.findByTestId(this.#cancelNewAttribute).click();
    return this;
  }

  createAttribute(name: string, displayName: string) {
    cy.findByTestId(this.#newAttributeNameInput).type(name);
    cy.findByTestId(this.#newAttributeDisplayNameInput).type(displayName);
    return this;
  }

  checkElementNotInList(name: string) {
    cy.get(this.#validatorsList).should("not.contain.text", name);
    return this;
  }

  saveAttributeCreation() {
    cy.findByTestId(this.#saveNewAttributeBtn).click();
    return this;
  }

  createAttributeNotRequiredWithPermissions(name: string, displayName: string) {
    cy.findByTestId(this.#newAttributeNameInput).type(name);
    cy.findByTestId(this.#newAttributeDisplayNameInput).type(displayName);
    cy.get(this.#newAttributeEnabledWhen).first().check();
    cy.findByTestId(this.#newAttributeUserEdit).first().check({ force: true });
    cy.findByTestId(this.#newAttributeUserView).first().check({ force: true });
    cy.findByTestId(this.#newAttributeAdminView).first().check({ force: true });
    return this;
  }

  createAttributeNotRequiredWithoutPermissions(
    name: string,
    displayName: string,
  ) {
    cy.findByTestId(this.#newAttributeNameInput).type(name);
    cy.findByTestId(this.#newAttributeDisplayNameInput).type(displayName);
    cy.get(this.#newAttributeEnabledWhen).first().check();
    cy.findByTestId(this.#newAttributeAdminEdit)
      .first()
      .uncheck({ force: true });

    return this;
  }

  createAttributeRequiredWithPermissions(name: string, displayName: string) {
    cy.findByTestId(this.#newAttributeNameInput).type(name);
    cy.findByTestId(this.#newAttributeDisplayNameInput).type(displayName);
    cy.get(this.#newAttributeEnabledWhen).first().check();
    cy.get(this.#newAttributeRequiredField).first().check({ force: true });
    cy.get(this.#newAttributeRequiredWhen).first().check({ force: true });
    cy.findByTestId(this.#newAttributeUserEdit).first().check({ force: true });
    cy.findByTestId(this.#newAttributeUserView).first().check({ force: true });
    cy.findByTestId(this.#newAttributeAdminView).first().check({ force: true });
    return this;
  }

  createAttributeGroup(name: string, displayName: string) {
    cy.get(this.#newAttributesGroupNameInput).type(name);
    cy.get(this.#newAttributesGroupDisplayNameInput).type(displayName);
    return this;
  }

  saveAttributesGroupCreation() {
    cy.findByTestId(this.#saveNewAttributesGroupBtn).click();
    return this;
  }

  selectElementInList(name: string) {
    cy.get(this.#validatorsList).contains(name).click();
    return this;
  }

  editAttribute(displayName: string) {
    cy.findByTestId(this.#newAttributeDisplayNameInput)
      .click()
      .clear()
      .type(displayName);
    cy.get(this.#newAttributeEnabledWhen).first().check();
    cy.get(this.#newAttributeEmptyValidators).contains("No validators.");
    cy.findByTestId(this.#newAttributeAnnotationBtn).click();
    cy.findByTestId(this.#newAttributeAnnotationKey).type("test");
    cy.findByTestId(this.#newAttributeAnnotationValue).type("123");
    return this;
  }

  addValidator() {
    cy.findByTestId(this.#addValidatorBtn).click();
    Select.selectItem(cy.get(this.#validatorRolesList), "email");
    cy.findByTestId(this.#saveValidatorBtn).click();
    return this;
  }

  removeValidator() {
    cy.findByTestId(this.#removeValidatorBtn).click();
    cy.findByTestId(this.#deleteValidatorBtn).click();
    return this;
  }

  cancelAddingValidator() {
    cy.findByTestId(this.#addValidatorBtn).click();
    Select.selectItem(cy.get(this.#validatorRolesList), "email");
    cy.findByTestId(this.#cancelAddingValidatorBtn).click();
    return this;
  }

  cancelRemovingValidator() {
    cy.findByTestId(this.#removeValidatorBtn).click();
    cy.findByTestId(this.#cancelRemovingValidatorBtn).click();
    return this;
  }

  #textArea() {
    return cy.get(".pf-c-code-editor__code textarea");
  }

  #getText() {
    return this.#textArea().get(".view-lines");
  }

  typeJSON(text: string) {
    this.#textArea().type(text, { force: true });
    return this;
  }

  shouldHaveText(text: string) {
    this.#getText().should("have.text", text);
    return this;
  }

  saveJSON() {
    cy.findAllByTestId("save").click();
    return this;
  }
}
