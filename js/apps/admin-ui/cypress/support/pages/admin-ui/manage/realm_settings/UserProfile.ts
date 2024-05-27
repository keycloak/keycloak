import Masthead from "../../Masthead";
import ValidatorConfigDialogue from "./ValidatorConfigDialogue";

export default class UserProfile {
  readonly masthead = new Masthead();
  readonly validatorConfigDialogue = new ValidatorConfigDialogue(this);

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
  #newAttributeEmptyValidators = ".kc-emptyValidators";
  #newAttributeAnnotationBtn = "annotations-add-row";
  #newAttributeAnnotationKey = "annotations.0.key";
  #newAttributeAnnotationValue = "annotations.0.value";
  #validatorsList = "tbody";
  #saveNewAttributeBtn = "attribute-create";
  #addValidatorBtn = "addValidator";
  #removeValidatorBtn = "deleteValidator";
  #deleteValidatorBtn = "confirm";
  #cancelRemovingValidatorBtn = "cancel";
  #newAttributeRequiredField = "input#kc-required.pf-v5-c-switch__input";
  #newAttributeUserEdit = "user-edit";
  #newAttributeAdminEdit = "admin-edit";
  #newAttributeUserView = "user-view";
  #newAttributeAdminView = "admin-view";
  #createAttributesGroupButton = "create-attributes-groups-action";
  #newAttributesGroupNameInput = "name";
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

  clickOnCreateAttributeButton() {
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

  setAttributeNames(name: string, displayName: string) {
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

  setAttributeRequired() {
    cy.get(this.#newAttributeRequiredField).first().check({ force: true });

    return this;
  }

  setAllAttributePermissions() {
    cy.findByTestId(this.#newAttributeUserEdit).first().check({ force: true });
    cy.findByTestId(this.#newAttributeUserView).first().check({ force: true });
    cy.findByTestId(this.#newAttributeAdminView).first().check({ force: true });

    return this;
  }

  setNoAttributePermissions() {
    cy.findByTestId(this.#newAttributeAdminEdit)
      .first()
      .uncheck({ force: true });

    return this;
  }

  clickOnCreatesAttributesGroupButton() {
    cy.findByTestId(this.#createAttributesGroupButton).click();
    return this;
  }

  createAttributeGroup(name: string, displayName: string) {
    cy.findByTestId(this.#newAttributesGroupNameInput).type(name);
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

  setAttributeGroup(group: string) {
    cy.get("#group").click();
    cy.get("#group")
      .parent()
      .get(".pf-v5-c-menu__list-item")
      .contains(group)
      .click();

    return this;
  }

  resetAttributeGroup() {
    return this.setAttributeGroup("None");
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

  addValidator(type: string) {
    this.clickAddValidator().selectValidatorType(type).clickSave();

    return this;
  }

  removeValidator() {
    cy.findByTestId(this.#removeValidatorBtn).click();
    cy.findByTestId(this.#deleteValidatorBtn).click();
    return this;
  }

  cancelAddingValidator(type: string) {
    this.clickAddValidator().selectValidatorType(type).clickCancel();

    return this;
  }

  clickAddValidator() {
    cy.findByTestId(this.#addValidatorBtn).click();

    return this.validatorConfigDialogue;
  }

  cancelRemovingValidator() {
    cy.findByTestId(this.#removeValidatorBtn).click();
    cy.findByTestId(this.#cancelRemovingValidatorBtn).click();
    return this;
  }

  #textArea() {
    return cy.get(".pf-v5-c-code-editor__code textarea");
  }

  #getText() {
    return this.#textArea().get(".view-lines");
  }

  typeJSON(text: string) {
    this.#textArea().type(text, { force: true });
    return this;
  }

  assertNotificationSaved() {
    this.masthead.checkNotificationMessage(
      "Success! User Profile configuration has been saved.",
    );

    return this;
  }

  assertNotificationUpdated() {
    this.masthead.checkNotificationMessage(
      "User profile settings successfully updated.",
    );

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
