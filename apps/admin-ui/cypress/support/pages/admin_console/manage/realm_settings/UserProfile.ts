export default class UserProfile {
  private userProfileTab = "rs-user-profile-tab";
  private attributesTab = "attributesTab";
  private attributesGroupTab = "attributesGroupTab";
  private jsonEditorTab = "jsonEditorTab";
  private createAttributeButton = "createAttributeBtn";
  private actionsDrpDwn = "actions-dropdown";
  private deleteDrpDwnOption = "deleteDropdownAttributeItem";
  private editDrpDwnOption = "editDropdownAttributeItem";
  private cancelNewAttribute = "attribute-cancel";
  private newAttributeNameInput = "attribute-name";
  private newAttributeDisplayNameInput = "attribute-display-name";
  private newAttributeEnabledWhen = 'input[name="enabledWhen"]';
  private newAttributeCheckboxes = 'input[type="checkbox"]';
  private newAttributeRequiredFor = 'input[name="roles"]';
  private newAttributeRequiredWhen = 'input[name="requiredWhen"]';
  private newAttributeEmptyValidators = ".kc-emptyValidators";
  private newAttributeAnnotationKey = 'input[name="annotations[0].key"]';
  private newAttributeAnnotationValue = 'input[name="annotations[0].value"]';
  private validatorRolesList = 'tbody [data-label="Role name"]';
  private validatorsList = 'tbody [data-label="name"]';
  private saveNewAttributeBtn = "attribute-create";
  private addValidatorBtn = "addValidator";
  private saveValidatorBtn = "save-validator-role-button";
  private removeValidatorBtn = "deleteValidator";
  private deleteValidatorBtn = "confirm";
  private cancelAddingValidatorBtn = "cancel-validator-role-button";
  private cancelRemovingValidatorBtn = "cancel";
  private validatorDialogCloseBtn = 'button[aria-label="Close"]';

  goToTab() {
    cy.findByTestId(this.userProfileTab).click();
    return this;
  }

  goToAttributesTab() {
    cy.findByTestId(this.attributesTab).click();
    return this;
  }

  goToAttributesGroupTab() {
    cy.findByTestId(this.attributesGroupTab).click();
    return this;
  }

  goToJsonEditorTab() {
    cy.findByTestId(this.jsonEditorTab).click();
    return this;
  }

  createAttributeButtonClick() {
    cy.findByTestId(this.createAttributeButton).click();
    return this;
  }

  selectDropdown() {
    cy.findByTestId(this.actionsDrpDwn).click();
    return this;
  }

  selectDeleteOption() {
    cy.findByTestId(this.deleteDrpDwnOption).click();
    return this;
  }

  selectEditOption() {
    cy.findByTestId(this.editDrpDwnOption).click();
    return this;
  }

  cancelAttributeCreation() {
    cy.findByTestId(this.cancelNewAttribute).click();
    return this;
  }

  createAttribute(name: string, displayName: string) {
    cy.findByTestId(this.newAttributeNameInput).type(name);
    cy.findByTestId(this.newAttributeDisplayNameInput).type(displayName);
    return this;
  }

  checkElementNotInList(name: string) {
    cy.get(this.validatorsList).should("not.contain.text", name);
    return this;
  }

  saveAttributeCreation() {
    cy.findByTestId(this.saveNewAttributeBtn).click();
    return this;
  }

  selectElementInList(name: string) {
    cy.get(this.validatorsList).contains(name).click();
    return this;
  }

  editAttribute(displayName: string) {
    cy.findByTestId(this.newAttributeDisplayNameInput)
      .click()
      .clear()
      .type(displayName);
    cy.get(this.newAttributeEnabledWhen).first().check();
    cy.get(this.newAttributeCheckboxes).check({ force: true });
    cy.get(this.newAttributeRequiredFor).first().check({ force: true });
    cy.get(this.newAttributeRequiredWhen).first().check();
    cy.get(this.newAttributeEmptyValidators).contains("No validators.");
    cy.get(this.newAttributeAnnotationKey).type("test");
    cy.get(this.newAttributeAnnotationValue).type("123");
    return this;
  }

  addValidator() {
    cy.get(this.validatorRolesList).contains("email").click();
    cy.findByTestId(this.saveValidatorBtn).click();
    cy.get(this.validatorDialogCloseBtn).click();
    return this;
  }

  removeValidator() {
    cy.findByTestId(this.removeValidatorBtn).click();
    cy.findByTestId(this.deleteValidatorBtn).click();
    return this;
  }

  cancelAddingValidator() {
    cy.findByTestId(this.addValidatorBtn).click();
    cy.get(this.validatorRolesList).contains("email").click();
    cy.findByTestId(this.cancelAddingValidatorBtn).click();
    return this;
  }

  cancelRemovingValidator() {
    cy.findByTestId(this.removeValidatorBtn).click();
    cy.findByTestId(this.cancelRemovingValidatorBtn).click();
    return this;
  }
}
